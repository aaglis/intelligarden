package com.smartgarden.backend.plant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlantWaterBalanceTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 11, 26, 8, 0, 0, 0, ZoneOffset.UTC);

    private Plant plant;

    @BeforeEach
    void setUp() {
        plant = new Plant();
        plant.setWaterCapacityMl(260);
        plant.setIrrigationThresholdPercent(30);
        plant.setIrrigationMl(180);
        plant.setLossCoefficient(3.0);
        plant.startEstimate(50, T0);
        plant.applyClimate(2.0, T0.plusSeconds(1)); // primeira leitura: guarda o VPD
    }

    @Test
    void shouldLoseKTimesVpdPerHour() {
        double before = plant.getEstimatedWaterMl();

        double loss = plant.applyClimate(2.0, T0.plusSeconds(1).plusHours(2));

        assertThat(loss).isCloseTo(3.0 * 2.0 * 2.0, within(1e-6));
        assertThat(plant.getEstimatedWaterMl()).isCloseTo(before - 12.0, within(1e-6));
        assertThat(plant.currentLossMlPerHour()).isCloseTo(6.0, within(1e-9));
    }

    @Test
    void shouldIgnoreOutOfOrderReading() {
        double before = plant.getEstimatedWaterMl();

        assertThat(plant.applyClimate(5.0, T0)).isZero();
        assertThat(plant.getEstimatedWaterMl()).isEqualTo(before);
    }

    @Test
    void shouldFlagNeedsWaterAtThresholdAndRespectCooldown() {
        // 130 mL - 78 mL (30 %) = 52 mL; com k=3 e VPD=2 são ~8,7 h
        OffsetDateTime later = T0.plusHours(9);
        plant.applyClimate(2.0, later);

        assertThat(plant.isNeedsWater()).isTrue();
        assertThat(plant.hoursToThreshold()).isZero();
        assertThat(plant.canAutoIrrigate(later)).isTrue();

        plant.addWater(180, false, later);
        assertThat(plant.isNeedsWater()).isFalse();
        assertThat(plant.getEstimatedWaterMl()).isLessThanOrEqualTo(260);

        plant.startEstimate(10, later);
        assertThat(plant.canAutoIrrigate(later.plusMinutes(10))).isFalse();
        assertThat(plant.canAutoIrrigate(later.plusMinutes(31))).isTrue();
    }

    @Test
    void shouldNotAutoIrrigateRealPot() {
        plant.setAutoIrrigation(false);
        plant.startEstimate(5, T0.plusHours(1));

        assertThat(plant.isNeedsWater()).isTrue();
        assertThat(plant.canAutoIrrigate(T0.plusHours(2))).isFalse();
    }

    @Test
    void shouldCompareMeasuredAndEstimatedLossBetweenWeighings() {
        plant.registerWeighing(900, false, T0.plusMinutes(1));
        plant.applyClimate(2.0, T0.plusSeconds(1).plusHours(5)); // estimado: 3 × 2 × 5 = 30 mL

        Plant.WeighingResult result = plant.registerWeighing(860, false, T0.plusHours(6)); // medido: 40 g

        assertThat(result.measuredLossMl()).isCloseTo(40.0, within(1e-9));
        assertThat(result.estimatedLossMl()).isCloseTo(30.0, within(0.1));
        assertThat(result.measuredCoefficient()).isCloseTo(4.0, within(0.02));
        assertThat(plant.getEstimatedLossSinceWeighingMl()).isZero();
    }

    @Test
    void shouldSkipComparisonWhenWateredBetweenWeighings() {
        plant.registerWeighing(900, false, T0.plusMinutes(1));
        plant.applyClimate(2.0, T0.plusHours(5));
        plant.addWater(100, false, T0.plusHours(5));

        Plant.WeighingResult result = plant.registerWeighing(980, false, T0.plusHours(6));

        assertThat(result.measuredLossMl()).isNull();
        assertThat(result.measuredCoefficient()).isNull();
    }

    @Test
    void shouldUseFullPotWeightToCorrectTheEstimate() {
        Plant.WeighingResult full = plant.registerWeighing(1100, true, T0.plusMinutes(1));
        assertThat(full.measuredPercent()).isEqualTo(100.0);
        assertThat(plant.waterPercent()).isCloseTo(100.0, within(1e-9));

        // 1100 - 970 = 130 g de água a menos -> 130 de 260 mL -> 50 %
        Plant.WeighingResult later = plant.registerWeighing(970, false, T0.plusHours(20));

        assertThat(later.measuredPercent()).isCloseTo(50.0, within(1e-9));
        assertThat(plant.getEstimatedWaterMl()).isCloseTo(130.0, within(1e-9));
    }

    @Test
    void shouldKeepPercentWhenCapacityChanges() {
        plant.startEstimate(40, T0);

        plant.changeCapacityKeepingPercent(500);

        assertThat(plant.waterPercent()).isCloseTo(40.0, within(1e-9));
        assertThat(plant.getEstimatedWaterMl()).isCloseTo(200.0, within(1e-9));
    }
}
