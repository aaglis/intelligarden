package com.smartgarden.backend.reading;

import com.smartgarden.backend.reading.dto.CreateReadingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Falhas do DHT22 (temperatura e umidade pela metade numa leitura isolada) não podem entrar no banco. */
@SpringBootTest
class ImplausibleReadingTest {

    @Autowired
    private EnvironmentalReadingService readingService;

    private final String deviceCode = "esp32-salto-" + UUID.randomUUID().toString().substring(0, 8);
    private final OffsetDateTime t0 = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS);

    private void reading(double temperature, double humidity, OffsetDateTime at) {
        readingService.createReading(new CreateReadingRequest(
                deviceCode, BigDecimal.valueOf(temperature), BigDecimal.valueOf(humidity), at, UUID.randomUUID()));
    }

    private long stored() {
        return readingService.listReadings(deviceCode, 0, 200, null, null).totalElements();
    }

    @Test
    void rejectsTheHalvedReadingAndKeepsTheNextOne() {
        reading(22.6, 48.5, t0);

        assertThatThrownBy(() -> reading(11.3, 24.1, t0.plusSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salto implausível");
        reading(22.6, 47.9, t0.plusSeconds(60));

        assertThat(stored()).isEqualTo(2);
    }

    @Test
    void acceptsARealChangeOnceThePreviousReadingIsOld() {
        reading(22.0, 50.0, t0);

        assertThatThrownBy(() -> reading(29.0, 50.0, t0.plusSeconds(30))).isInstanceOf(IllegalArgumentException.class);
        reading(29.0, 50.0, t0.plusMinutes(3));

        assertThat(stored()).isEqualTo(2);
    }

    @Test
    void acceptsNormalVariation() {
        reading(22.0, 50.0, t0);
        reading(22.4, 51.5, t0.plusSeconds(30));
        reading(23.1, 49.0, t0.plusSeconds(60));

        assertThat(stored()).isEqualTo(3);
    }
}
