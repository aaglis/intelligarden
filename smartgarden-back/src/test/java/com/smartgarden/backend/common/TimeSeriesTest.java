package com.smartgarden.backend.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/** Os gráficos precisam cobrir o período pedido, não só os pontos mais recentes. */
class TimeSeriesTest {

    private static final OffsetDateTime START = OffsetDateTime.of(2026, 11, 26, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = START.plusHours(24);

    private static List<OffsetDateTime> every(Duration step, OffsetDateTime from, OffsetDateTime to) {
        List<OffsetDateTime> out = new ArrayList<>();
        for (OffsetDateTime t = from; t.isBefore(to); t = t.plus(step)) {
            out.add(t);
        }
        return out;
    }

    private static List<OffsetDateTime> spread(List<OffsetDateTime> readings, int buckets) {
        return TimeSeries.spread(readings, Function.identity(), START, END, buckets);
    }

    @Test
    void coversTheWholeWindowWithEvenlySpacedPoints() {
        List<OffsetDateTime> readings = every(Duration.ofSeconds(30), START, END); // 2880 leituras, como o ESP32

        List<OffsetDateTime> points = spread(readings, 288);

        assertThat(points).hasSize(288);
        assertThat(points.get(0)).isBefore(START.plusMinutes(5));
        assertThat(points.get(points.size() - 1)).isEqualTo(readings.get(readings.size() - 1));
        for (int i = 1; i < points.size(); i++) {
            assertThat(Duration.between(points.get(i - 1), points.get(i))).isEqualTo(Duration.ofMinutes(5));
        }
    }

    @Test
    void keepsEverythingWhenThereAreFewerReadingsThanPoints() {
        List<OffsetDateTime> readings = every(Duration.ofMinutes(30), START, END); // 48 leituras

        assertThat(spread(readings, 120)).isEqualTo(readings);
    }

    @Test
    void leavesTheGapWhenTheSensorWasOffline() {
        List<OffsetDateTime> readings = new ArrayList<>(every(Duration.ofSeconds(30), START, START.plusHours(1)));
        readings.addAll(every(Duration.ofSeconds(30), END.minusHours(1), END));

        List<OffsetDateTime> points = spread(readings, 48); // faixas de 30 min

        assertThat(points).hasSize(4); // 2 faixas em cada hora com dados, nenhuma inventada no buraco
        assertThat(points).noneMatch(t -> t.isAfter(START.plusHours(1)) && t.isBefore(END.minusHours(1)));
    }
}
