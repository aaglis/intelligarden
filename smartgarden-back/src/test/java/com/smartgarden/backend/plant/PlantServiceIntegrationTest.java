package com.smartgarden.backend.plant;

import com.smartgarden.backend.device.DeviceService;
import com.smartgarden.backend.device.dto.CreateDeviceRequest;
import com.smartgarden.backend.plant.dto.CreatePlantRecordRequest;
import com.smartgarden.backend.plant.dto.CreatePlantRequest;
import com.smartgarden.backend.plant.dto.UpdatePlantRequest;
import com.smartgarden.backend.reading.EnvironmentalReadingService;
import com.smartgarden.backend.reading.dto.CreateReadingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@RecordApplicationEvents
class PlantServiceIntegrationTest {

    @Autowired
    private PlantService plantService;

    @Autowired
    private EnvironmentalReadingService readingService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ApplicationEvents applicationEvents;

    private String deviceCode;
    private String plantCode;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        deviceCode = "esp32-plant-" + suffix;
        plantCode = "teste-" + suffix;
        deviceService.createDevice(new CreateDeviceRequest(deviceCode, "Sensor de teste", null));
    }

    @Test
    void shouldDrainWaterFromSensorReadings() {
        createPlant(80.0, true);
        OffsetDateTime t0 = OffsetDateTime.now().minusHours(3).truncatedTo(ChronoUnit.SECONDS);

        reading(25.0, 50.0, t0);
        reading(25.0, 50.0, t0.plusHours(1));

        var plant = plantService.getPlant(plantCode);
        // 200 mL × 80 % = 160 mL; perda = k (2,0) × VPD (1,584) × 1 h ≈ 3,17 mL
        assertThat(plant.estimatedWaterMl()).isCloseTo(160.0 - 3.17, within(0.05));
        assertThat(plant.lastVpdKpa()).isCloseTo(1.584, within(0.001));
        assertThat(plant.lossMlPerHour()).isCloseTo(3.17, within(0.01));
        assertThat(applicationEvents.stream(PlantsUpdatedEvent.class)).isNotEmpty();
    }

    @Test
    void shouldIrrigateVirtualPlantAutomaticallyAtThreshold() {
        createPlant(31.0, true);
        OffsetDateTime t0 = OffsetDateTime.now().minusHours(3).truncatedTo(ChronoUnit.SECONDS);

        reading(30.0, 30.0, t0);
        reading(30.0, 30.0, t0.plusHours(1)); // VPD ≈ 2,97 kPa -> perde ~5,9 mL e passa de 31 % para ~28 %

        var records = plantService.listRecords(plantCode, 10);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).type()).isEqualTo(PlantRecordType.IRRIGATION);
        assertThat(records.get(0).automatic()).isTrue();
        assertThat(records.get(0).waterMl()).isEqualTo(140);
        assertThat(plantService.getPlant(plantCode).needsWater()).isFalse();
        assertThat(applicationEvents.stream(PlantIrrigatedEvent.class)).hasSize(1);
    }

    @Test
    void shouldOnlyFlagRealPotAndRefuseSimulatedIrrigation() {
        createPlant(31.0, false);
        OffsetDateTime t0 = OffsetDateTime.now().minusHours(3).truncatedTo(ChronoUnit.SECONDS);

        reading(30.0, 30.0, t0);
        reading(30.0, 30.0, t0.plusHours(1));

        assertThat(plantService.getPlant(plantCode).needsWater()).isTrue();
        assertThat(plantService.listRecords(plantCode, 10)).isEmpty();
        assertThatThrownBy(() -> plantService.irrigate(plantCode))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        plantService.addRecord(plantCode, new CreatePlantRecordRequest(PlantRecordType.WATERING, null, null, 150, true, null));
        var plant = plantService.getPlant(plantCode);
        assertThat(plant.estimatedWaterPercent()).isEqualTo(100.0);
        assertThat(plant.needsWater()).isFalse();
    }

    @Test
    void shouldCalibrateFromWeighings() {
        createPlant(100.0, false);
        OffsetDateTime t0 = OffsetDateTime.now().minusHours(6).truncatedTo(ChronoUnit.SECONDS);
        reading(25.0, 50.0, t0);

        plantService.addRecord(plantCode, new CreatePlantRecordRequest(PlantRecordType.WEIGHING, 1200.0, true, null, null, null));
        reading(25.0, 50.0, t0.plusHours(5)); // estimado: 2,0 × 1,584 × 5 ≈ 15,8 mL
        var weighing = plantService.addRecord(plantCode,
                new CreatePlantRecordRequest(PlantRecordType.WEIGHING, 1180.0, false, null, null, "pesado às 8h"));

        assertThat(weighing.measuredLossMl()).isEqualTo(20.0);
        assertThat(weighing.estimatedLossMl()).isCloseTo(15.8, within(0.1));
        assertThat(weighing.measuredWaterPercent()).isEqualTo(90.0);
        var plant = plantService.getPlant(plantCode);
        assertThat(plant.calibrationSamples()).isEqualTo(1);
        assertThat(plant.suggestedCoefficient()).isCloseTo(20.0 / (1.584 * 5), within(0.01));
        assertThat(plant.meanErrorPercent()).isCloseTo(20.8, within(0.5));
        assertThat(plant.estimatedWaterMl()).isEqualTo(180.0);

        var updated = plantService.updatePlant(plantCode, new UpdatePlantRequest(
                null, null, null, null, null, null, null, null, plant.suggestedCoefficient(), null, null, null, null, null));
        assertThat(updated.lossCoefficient()).isEqualTo(plant.suggestedCoefficient());
    }

    @Test
    void shouldRejectObservationWithoutNote() {
        createPlant(80.0, true);

        assertThatThrownBy(() -> plantService.addRecord(plantCode,
                new CreatePlantRecordRequest(PlantRecordType.OBSERVATION, null, null, null, null, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void createPlant(double initialPercent, boolean autoIrrigation) {
        plantService.createPlant(new CreatePlantRequest(
                plantCode, "Planta de teste", null, null, deviceCode,
                200.0, 30.0, null, 2.0, autoIrrigation,
                18.0, 30.0, 40.0, 70.0, initialPercent
        ));
    }

    private void reading(double temperature, double humidity, OffsetDateTime at) {
        readingService.createReading(new CreateReadingRequest(
                deviceCode, BigDecimal.valueOf(temperature), BigDecimal.valueOf(humidity), at, UUID.randomUUID()));
    }
}
