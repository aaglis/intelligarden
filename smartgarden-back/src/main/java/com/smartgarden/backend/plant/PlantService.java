package com.smartgarden.backend.plant;

import com.smartgarden.backend.common.ResourceAlreadyExistsException;
import com.smartgarden.backend.common.TimeSeries;
import com.smartgarden.backend.device.Device;
import com.smartgarden.backend.device.DeviceService;
import com.smartgarden.backend.plant.dto.CreatePlantRecordRequest;
import com.smartgarden.backend.plant.dto.CreatePlantRequest;
import com.smartgarden.backend.plant.dto.PlantRecordResponse;
import com.smartgarden.backend.plant.dto.PlantResponse;
import com.smartgarden.backend.plant.dto.PlantWaterHistoryResponse;
import com.smartgarden.backend.plant.dto.UpdatePlantRequest;
import com.smartgarden.backend.reading.dto.ReadingResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PlantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantService.class);
    static final int CALIBRATION_WINDOW = 10;
    /** Leituras com horário muito à frente do relógio do servidor (ESP sem NTP) não entram no balanço. */
    static final long MAX_CLOCK_SKEW_MINUTES = 5;
    /** Intervalo entre dois pontos da curva da água; regas, irrigações e pesagens gravam pontos na hora. */
    static final Duration WATER_HISTORY_STEP = Duration.ofMinutes(10);

    private final PlantRepository plantRepository;
    private final PlantRecordRepository recordRepository;
    private final PlantWaterPointRepository waterHistoryRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    public PlantService(
            PlantRepository plantRepository,
            PlantRecordRepository recordRepository,
            PlantWaterPointRepository waterHistoryRepository,
            DeviceService deviceService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.plantRepository = plantRepository;
        this.recordRepository = recordRepository;
        this.waterHistoryRepository = waterHistoryRepository;
        this.deviceService = deviceService;
        this.eventPublisher = eventPublisher;
    }

    // ---------------------------------------------------------------- balanço de água

    /**
     * Desconta a água de cada planta do sensor com a leitura nova e dispara a irrigação simulada
     * das plantas virtuais que chegaram no limite. Roda numa transação própria, depois que a leitura foi salva.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyReading(ReadingResponse reading) {
        if (reading.deviceId() == null || reading.recordedAt() == null) {
            return;
        }
        OffsetDateTime at = reading.recordedAt();
        if (at.isAfter(OffsetDateTime.now().plusMinutes(MAX_CLOCK_SKEW_MINUTES))) {
            LOGGER.warn("Leitura {} com horario no futuro ({}); fora do balanco de agua", reading.id(), at);
            return;
        }
        List<Plant> plants = plantRepository.findWithLockByDeviceIdAndActiveTrueOrderByIdAsc(reading.deviceId());
        if (plants.isEmpty()) {
            return;
        }

        double vpd = WaterModel.vpdKpa(reading.temperatureC().doubleValue(), reading.humidityPercent().doubleValue());
        for (Plant plant : plants) {
            plant.applyClimate(vpd, at);
            if (plant.canAutoIrrigate(at)) {
                LOGGER.info("Irrigacao automatica: {} chegou a {}% (limite {}%)",
                        plant.getPlantCode(), Math.round(plant.waterPercent()), plant.getIrrigationThresholdPercent());
                irrigateLocked(plant, true, at); // grava o ponto antes e o depois
            } else {
                recordWater(plant, at, false);
            }
        }
        eventPublisher.publishEvent(new PlantsUpdatedEvent(
                reading.deviceCode(),
                plants.stream().map(PlantResponse::fromEntity).toList()
        ));
    }

    // ---------------------------------------------------------------- registros

    @Transactional
    public PlantRecordResponse addRecord(String plantCode, CreatePlantRecordRequest request) {
        Plant plant = lock(plantCode);
        OffsetDateTime now = OffsetDateTime.now();
        PlantRecord record = PlantRecord.of(plant, request.type(), now);
        record.setNote(blankToNull(request.note()));
        boolean changesWater = request.type() == PlantRecordType.WEIGHING || request.type() == PlantRecordType.WATERING;
        if (changesWater) {
            recordWater(plant, now, true);
        }

        switch (request.type()) {
            case WEIGHING -> {
                if (request.weightGrams() == null) {
                    throw new IllegalArgumentException("weightGrams: informe o peso do vaso em gramas.");
                }
                boolean fullPot = Boolean.TRUE.equals(request.fullPot());
                Plant.WeighingResult result = plant.registerWeighing(request.weightGrams(), fullPot, now);
                record.setWeightG(request.weightGrams());
                record.setFullPot(fullPot);
                record.setMeasuredWaterPercent(result.measuredPercent());
                record.setEstimatedLossMl(result.estimatedLossMl());
                record.setMeasuredLossMl(result.measuredLossMl());
                record.setMeasuredCoefficient(result.measuredCoefficient());
            }
            case WATERING -> {
                if (request.waterMl() == null) {
                    throw new IllegalArgumentException("waterMl: informe quanto de água foi colocado, em mL.");
                }
                boolean drained = Boolean.TRUE.equals(request.drained());
                plant.addWater(request.waterMl(), drained, now);
                record.setWaterMl(request.waterMl());
                record.setDrained(drained);
            }
            case OBSERVATION -> {
                if (record.getNote() == null) {
                    throw new IllegalArgumentException("note: escreva a observação.");
                }
            }
            case IRRIGATION -> throw new IllegalArgumentException(
                    "type: a irrigação simulada é disparada em POST /api/plants/{code}/irrigations.");
        }

        if (changesWater) {
            recordWater(plant, now, true);
        }
        PlantRecord saved = recordRepository.save(record);
        if (saved.getMeasuredLossMl() != null) {
            updateCalibration(plant);
        }
        publishUpdated(plant);
        return PlantRecordResponse.fromEntity(saved);
    }

    /** Botão "Simular irrigação". Vasos reais (autoIrrigation=false) não aceitam: a rega deles é registrada pela equipe. */
    @Transactional
    public PlantRecordResponse irrigate(String plantCode) {
        Plant plant = lock(plantCode);
        if (!plant.isAutoIrrigation()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta planta é um vaso real: registre a rega feita pela equipe em vez de simular.");
        }
        PlantRecord record = irrigateLocked(plant, false, OffsetDateTime.now());
        publishUpdated(plant);
        return PlantRecordResponse.fromEntity(record);
    }

    @Transactional(readOnly = true)
    public List<PlantRecordResponse> listRecords(String plantCode, int limit) {
        Plant plant = find(plantCode);
        int sanitized = Math.min(Math.max(limit, 1), 100);
        return recordRepository.findByPlantIdOrderByRecordedAtDescIdDesc(plant.getId(), PageRequest.of(0, sanitized))
                .stream()
                .map(PlantRecordResponse::fromEntity)
                .toList();
    }

    /** Curva da água estimada no período, já espalhada para no máximo {@code limit} pontos. */
    @Transactional(readOnly = true)
    public PlantWaterHistoryResponse waterHistory(String plantCode, int hours, int limit) {
        Plant plant = find(plantCode);
        OffsetDateTime end = OffsetDateTime.now();
        OffsetDateTime start = end.minusHours(Math.min(Math.max(hours, 1), 24 * 31));
        int sanitizedLimit = Math.min(Math.max(limit, 2), 1000);
        List<PlantWaterHistoryResponse.Point> points = TimeSeries.spread(
                        waterHistoryRepository.findByPlantIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(plant.getId(), start, end),
                        PlantWaterPoint::getRecordedAt, start, end, sanitizedLimit)
                .stream()
                .map(p -> new PlantWaterHistoryResponse.Point(p.getRecordedAt(), p.getWaterMl(), p.getWaterPercent()))
                .toList();
        return new PlantWaterHistoryResponse(plant.getPlantCode(), plant.getWaterCapacityMl(),
                plant.getIrrigationThresholdPercent(), start, end, points);
    }

    // ---------------------------------------------------------------- cadastro

    @Transactional(readOnly = true)
    public List<PlantResponse> listPlants(String deviceCode) {
        List<Plant> plants = deviceCode == null || deviceCode.isBlank()
                ? plantRepository.findAllByOrderByIdAsc()
                : plantRepository.findByDeviceDeviceCodeOrderByIdAsc(deviceCode.trim());
        return plants.stream().map(PlantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public PlantResponse getPlant(String plantCode) {
        return PlantResponse.fromEntity(find(plantCode));
    }

    @Transactional
    public PlantResponse createPlant(CreatePlantRequest request) {
        if (plantRepository.existsByPlantCode(request.code())) {
            throw new ResourceAlreadyExistsException("Ja existe uma planta com o codigo informado.");
        }
        Plant plant = new Plant();
        plant.setPlantCode(request.code());
        plant.setName(request.name().trim());
        plant.setScientificName(blankToNull(request.scientificName()));
        plant.setSpot(blankToNull(request.spot()));
        plant.setDevice(device(request.deviceCode()));
        plant.setWaterCapacityMl(request.waterCapacityMl());
        plant.setIrrigationThresholdPercent(request.irrigationThresholdPercent());
        plant.setIrrigationMl(request.irrigationMl() != null
                ? request.irrigationMl()
                : defaultIrrigationMl(request.waterCapacityMl(), request.irrigationThresholdPercent()));
        plant.setLossCoefficient(request.lossCoefficient());
        plant.setAutoIrrigation(request.autoIrrigation() == null || request.autoIrrigation());
        plant.setTemperatureMinC(request.temperatureMinC());
        plant.setTemperatureMaxC(request.temperatureMaxC());
        plant.setHumidityMinPercent(request.humidityMinPercent());
        plant.setHumidityMaxPercent(request.humidityMaxPercent());
        checkRanges(plant);
        plant.startEstimate(request.initialWaterPercent() != null ? request.initialWaterPercent() : 100.0, OffsetDateTime.now());

        Plant saved = plantRepository.save(plant);
        recordWater(saved, OffsetDateTime.now(), true);
        publishUpdated(saved);
        return PlantResponse.fromEntity(saved);
    }

    @Transactional
    public PlantResponse updatePlant(String plantCode, UpdatePlantRequest request) {
        Plant plant = lock(plantCode);
        if (request.name() != null) {
            plant.setName(request.name().trim());
        }
        if (request.scientificName() != null) {
            plant.setScientificName(blankToNull(request.scientificName()));
        }
        if (request.spot() != null) {
            plant.setSpot(blankToNull(request.spot()));
        }
        if (request.deviceCode() != null) {
            plant.setDevice(device(request.deviceCode()));
        }
        if (request.active() != null) {
            plant.setActive(request.active());
        }
        if (request.waterCapacityMl() != null) {
            plant.changeCapacityKeepingPercent(request.waterCapacityMl());
        }
        if (request.irrigationThresholdPercent() != null) {
            plant.setIrrigationThresholdPercent(request.irrigationThresholdPercent());
        }
        if (request.irrigationMl() != null) {
            plant.setIrrigationMl(request.irrigationMl());
        }
        if (request.lossCoefficient() != null) {
            plant.setLossCoefficient(request.lossCoefficient());
        }
        if (request.autoIrrigation() != null) {
            plant.setAutoIrrigation(request.autoIrrigation());
        }
        if (request.temperatureMinC() != null) {
            plant.setTemperatureMinC(request.temperatureMinC());
        }
        if (request.temperatureMaxC() != null) {
            plant.setTemperatureMaxC(request.temperatureMaxC());
        }
        if (request.humidityMinPercent() != null) {
            plant.setHumidityMinPercent(request.humidityMinPercent());
        }
        if (request.humidityMaxPercent() != null) {
            plant.setHumidityMaxPercent(request.humidityMaxPercent());
        }
        checkRanges(plant);
        plant.refreshNeedsWater();
        publishUpdated(plant);
        return PlantResponse.fromEntity(plant);
    }

    // ---------------------------------------------------------------- apoio

    private PlantRecord irrigateLocked(Plant plant, boolean automatic, OffsetDateTime at) {
        PlantRecord record = PlantRecord.of(plant, PlantRecordType.IRRIGATION, at);
        record.setAutomatic(automatic);
        record.setWaterMl(plant.getIrrigationMl());
        recordWater(plant, at, true);
        plant.addWater(plant.getIrrigationMl(), false, at);
        recordWater(plant, at, true);
        PlantRecord saved = recordRepository.save(record);

        String deviceCode = plant.getDevice() == null ? null : plant.getDevice().getDeviceCode();
        eventPublisher.publishEvent(new PlantIrrigatedEvent(
                deviceCode,
                PlantResponse.fromEntity(plant),
                PlantRecordResponse.fromEntity(saved)
        ));
        return saved;
    }

    /**
     * Guarda um ponto da curva da água. Nas leituras, só se o último ponto estiver a 10 minutos ou mais (para frente
     * ou para trás); com {@code force}, sempre: regas, irrigações e pesagens gravam um ponto antes e outro depois,
     * para o degrau ficar no lugar certo.
     */
    private void recordWater(Plant plant, OffsetDateTime at, boolean force) {
        OffsetDateTime last = plant.getWaterHistoryAt();
        if (!force && last != null && Duration.between(last, at).abs().compareTo(WATER_HISTORY_STEP) < 0) {
            return;
        }
        waterHistoryRepository.save(PlantWaterPoint.of(plant, at));
        plant.setWaterHistoryAt(at);
    }

    /** k sugerido = mediana dos k medidos nas últimas pesagens; erro = |estimado − medido| / medido. */
    private void updateCalibration(Plant plant) {
        List<PlantRecord> recent = recordRepository.findByPlantIdAndTypeAndMeasuredLossMlIsNotNullOrderByRecordedAtDesc(
                plant.getId(), PlantRecordType.WEIGHING, PageRequest.of(0, CALIBRATION_WINDOW));
        List<Double> coefficients = recent.stream()
                .map(PlantRecord::getMeasuredCoefficient)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<Double> errors = recent.stream()
                .filter(r -> r.getEstimatedLossMl() != null && r.getMeasuredLossMl() > 0)
                .map(r -> Math.abs(r.getEstimatedLossMl() - r.getMeasuredLossMl()) / r.getMeasuredLossMl() * 100.0)
                .toList();
        plant.updateCalibration(
                coefficients.size(),
                median(coefficients),
                errors.isEmpty() ? null : errors.stream().mapToDouble(Double::doubleValue).average().orElseThrow()
        );
    }

    static Double median(List<Double> sorted) {
        if (sorted.isEmpty()) {
            return null;
        }
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    static int defaultIrrigationMl(double capacityMl, double thresholdPercent) {
        return (int) Math.max(1, Math.round(capacityMl * (100.0 - thresholdPercent) / 100.0));
    }

    private void publishUpdated(Plant plant) {
        String deviceCode = plant.getDevice() == null ? null : plant.getDevice().getDeviceCode();
        eventPublisher.publishEvent(new PlantsUpdatedEvent(deviceCode, List.of(PlantResponse.fromEntity(plant))));
    }

    private void checkRanges(Plant plant) {
        if (plant.getTemperatureMinC() != null && plant.getTemperatureMaxC() != null
                && plant.getTemperatureMinC() > plant.getTemperatureMaxC()) {
            throw new IllegalArgumentException("temperatureMinC: deve ser menor que temperatureMaxC.");
        }
        if (plant.getHumidityMinPercent() != null && plant.getHumidityMaxPercent() != null
                && plant.getHumidityMinPercent() > plant.getHumidityMaxPercent()) {
            throw new IllegalArgumentException("humidityMinPercent: deve ser menor que humidityMaxPercent.");
        }
    }

    private Device device(String deviceCode) {
        return deviceService.findByCode(deviceCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("deviceCode: sensor nao encontrado: " + deviceCode));
    }

    private Plant find(String plantCode) {
        return plantRepository.findByPlantCode(plantCode)
                .orElseThrow(() -> new EntityNotFoundException("Planta nao encontrada: " + plantCode));
    }

    private Plant lock(String plantCode) {
        return plantRepository.findWithLockByPlantCode(plantCode)
                .orElseThrow(() -> new EntityNotFoundException("Planta nao encontrada: " + plantCode));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
