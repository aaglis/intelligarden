package com.smartgarden.backend.reading;

import com.smartgarden.backend.common.PageResponse;
import com.smartgarden.backend.device.Device;
import com.smartgarden.backend.device.DeviceService;
import com.smartgarden.backend.reading.dto.CreateReadingRequest;
import com.smartgarden.backend.reading.dto.ReadingHistoryResponse;
import com.smartgarden.backend.reading.dto.ReadingResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class EnvironmentalReadingService {

    private final EnvironmentalReadingRepository readingRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    public EnvironmentalReadingService(
            EnvironmentalReadingRepository readingRepository,
            DeviceService deviceService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.readingRepository = readingRepository;
        this.deviceService = deviceService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReadingResponse createReading(CreateReadingRequest request) {
        if (request.messageId() != null) {
            var existing = readingRepository.findByMessageId(request.messageId());
            if (existing.isPresent()) {
                return ReadingResponse.fromEntity(existing.get());
            }
        }

        Device device = deviceService.findOrCreateByCode(request.deviceCode());
        OffsetDateTime recordedAt = request.recordedAt() != null ? request.recordedAt() : OffsetDateTime.now();
        readingRepository.findFirstByDeviceDeviceCodeOrderByRecordedAtDesc(request.deviceCode())
                .ifPresent(previous -> rejectImplausibleJump(previous, request.temperatureC(), request.humidityPercent(), recordedAt));

        EnvironmentalReading reading = new EnvironmentalReading();
        reading.setDevice(device);
        reading.setTemperatureC(request.temperatureC());
        reading.setHumidityPercent(request.humidityPercent());
        reading.setRecordedAt(recordedAt);
        reading.setMessageId(request.messageId());

        EnvironmentalReading saved = readingRepository.save(reading);
        ReadingResponse response = ReadingResponse.fromEntity(saved);
        eventPublisher.publishEvent(new ReadingCreatedEvent(response));
        return response;
    }

    /** Maior variação aceita entre duas leituras próximas: um cômodo não muda 5 °C ou 20 pontos de umidade em segundos. */
    static final double MAX_TEMPERATURE_JUMP_C = 5.0;
    static final double MAX_HUMIDITY_JUMP_PERCENT = 20.0;
    /** Só compara com uma leitura recente; se o ambiente mudou de verdade, a leitura nova volta a ser aceita depois disso. */
    static final Duration JUMP_WINDOW = Duration.ofMinutes(2);

    /**
     * Recusa uma leitura que salta demais em relação à anterior. O DHT22 às vezes entrega temperatura e umidade pela
     * metade (um bit deslocado passa na conferência do próprio sensor); sem esta trava, o valor iria para o histórico,
     * para os relatórios e para a estimativa de água. Pelo MQTT a leitura é descartada com um aviso no log.
     */
    static void rejectImplausibleJump(EnvironmentalReading previous, BigDecimal temperatureC, BigDecimal humidityPercent,
                                      OffsetDateTime recordedAt) {
        if (previous.getRecordedAt() == null || temperatureC == null || humidityPercent == null
                || Duration.between(previous.getRecordedAt(), recordedAt).abs().compareTo(JUMP_WINDOW) >= 0) {
            return;
        }
        double temperatureJump = Math.abs(temperatureC.doubleValue() - previous.getTemperatureC().doubleValue());
        double humidityJump = Math.abs(humidityPercent.doubleValue() - previous.getHumidityPercent().doubleValue());
        if (temperatureJump > MAX_TEMPERATURE_JUMP_C || humidityJump > MAX_HUMIDITY_JUMP_PERCENT) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "Leitura descartada: salto implausível em relação à anterior (%.1f °C e %.1f pontos de umidade em menos de %d min).",
                    temperatureJump, humidityJump, JUMP_WINDOW.toMinutes()));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReadingResponse> listReadings(
            String deviceCode,
            int page,
            int size,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageRequest = PageRequest.of(
                sanitizedPage,
                sanitizedSize,
                Sort.by(Sort.Direction.DESC, "recordedAt")
        );

        String normalizedDeviceCode = (deviceCode == null || deviceCode.isBlank()) ? null : deviceCode;
        Specification<EnvironmentalReading> specification = Specification.allOf(
                hasDeviceCode(normalizedDeviceCode),
                recordedAtGreaterThanOrEqualTo(startAt),
                recordedAtLessThanOrEqualTo(endAt)
        );

        Page<EnvironmentalReading> readings = readingRepository.findAll(specification, pageRequest);

        return PageResponse.from(readings.map(ReadingResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public ReadingResponse getLatestReading(String deviceCode) {
        EnvironmentalReading reading = readingRepository.findFirstByDeviceDeviceCodeOrderByRecordedAtDesc(deviceCode)
                .orElseThrow(() -> new EntityNotFoundException("Nenhuma leitura encontrada para o dispositivo informado."));
        return ReadingResponse.fromEntity(reading);
    }

    @Transactional(readOnly = true)
    public ReadingHistoryResponse getReadingHistory(
            String deviceCode,
            int hours,
            int limit,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new EntityNotFoundException("Informe o deviceCode para consultar o histórico.");
        }

        OffsetDateTime windowEnd = endAt != null ? endAt : OffsetDateTime.now();
        OffsetDateTime windowStart = startAt != null ? startAt : windowEnd.minusHours(Math.max(hours, 1));
        int sanitizedLimit = Math.min(Math.max(limit, 2), 500);

        Specification<EnvironmentalReading> specification = Specification.allOf(
                hasDeviceCode(deviceCode),
                recordedAtGreaterThanOrEqualTo(windowStart),
                recordedAtLessThanOrEqualTo(windowEnd)
        );

        List<EnvironmentalReading> history = readingRepository.findAll(
                specification,
                Sort.by(Sort.Direction.ASC, "recordedAt")
        );

        List<ReadingResponse> points = spreadOverWindow(history, EnvironmentalReading::getRecordedAt, windowStart, windowEnd, sanitizedLimit)
                .stream()
                .map(ReadingResponse::fromEntity)
                .toList();

        String deviceName = points.isEmpty()
                ? deviceService.findByCode(deviceCode).map(Device::getName).orElse(deviceCode)
                : points.get(0).deviceName();
        return new ReadingHistoryResponse(
                deviceCode,
                deviceName,
                windowStart,
                windowEnd,
                points.size(),
                points
        );
    }

    /**
     * Espalha os pontos pelo período: divide a janela em {@code buckets} faixas de tempo iguais e fica com a última
     * leitura de cada faixa. Antes, com mais leituras que o limite, só as mais recentes voltavam (24 h pedidas viravam
     * as últimas ~4 h). Faixas sem leitura (sensor desligado) ficam vazias, e o gráfico mostra o buraco.
     *
     * @param sorted leituras em ordem crescente de tempo
     */
    static <T> List<T> spreadOverWindow(List<T> sorted, Function<T, OffsetDateTime> time,
                                        OffsetDateTime start, OffsetDateTime end, int buckets) {
        if (sorted.size() <= buckets) {
            return sorted;
        }
        long startMs = start.toInstant().toEpochMilli();
        long span = Math.max(1, end.toInstant().toEpochMilli() - startMs);
        List<T> spread = new ArrayList<>(buckets);
        int current = -1;
        for (T item : sorted) {
            long offset = time.apply(item).toInstant().toEpochMilli() - startMs;
            int bucket = (int) Math.min(buckets - 1, Math.max(0, offset * buckets / span));
            if (bucket == current) {
                spread.set(spread.size() - 1, item);
            } else {
                spread.add(item);
                current = bucket;
            }
        }
        return spread;
    }

    private Specification<EnvironmentalReading> hasDeviceCode(String deviceCode) {
        return (root, query, criteriaBuilder) -> {
            if (deviceCode == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.join("device").get("deviceCode"), deviceCode);
        };
    }

    private Specification<EnvironmentalReading> recordedAtGreaterThanOrEqualTo(OffsetDateTime startAt) {
        return (root, query, criteriaBuilder) -> {
            if (startAt == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("recordedAt"), startAt);
        };
    }

    private Specification<EnvironmentalReading> recordedAtLessThanOrEqualTo(OffsetDateTime endAt) {
        return (root, query, criteriaBuilder) -> {
            if (endAt == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("recordedAt"), endAt);
        };
    }
}
