package com.smartgarden.backend.plant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WaterModelTest {

    @Test
    void shouldMatchFao56SaturationVaporPressure() {
        // FAO-56, tabela 2.3: es(25 °C) = 3,168 kPa; es(30 °C) = 4,243 kPa
        assertThat(WaterModel.saturationVaporPressureKpa(25)).isCloseTo(3.168, within(0.001));
        assertThat(WaterModel.saturationVaporPressureKpa(30)).isCloseTo(4.243, within(0.001));
    }

    @Test
    void shouldComputeVpdFromTemperatureAndHumidity() {
        assertThat(WaterModel.vpdKpa(25, 50)).isCloseTo(1.584, within(0.001));
        assertThat(WaterModel.vpdKpa(25, 100)).isZero();
        assertThat(WaterModel.vpdKpa(25, 120)).isZero();
    }

    @Test
    void shouldIntegrateVpdWithTrapezoidAndCapLongGaps() {
        assertThat(WaterModel.vpdHours(1.0, 2.0, 2.0)).isCloseTo(3.0, within(1e-9));
        assertThat(WaterModel.vpdHours(null, 2.0, 0.5)).isCloseTo(1.0, within(1e-9));
        assertThat(WaterModel.vpdHours(1.0, 1.0, 48.0)).isCloseTo(WaterModel.MAX_GAP_HOURS, within(1e-9));
    }

    @Test
    void shouldMapIrrigationVolumeToLedSeconds() {
        assertThat(WaterModel.ledSecondsFor(180)).isEqualTo(18);
        assertThat(WaterModel.ledSecondsFor(3)).isEqualTo(1);
        assertThat(WaterModel.ledSecondsFor(2000)).isEqualTo(60);
    }
}
