package com.smartgarden.backend.common;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Utilitários para séries de pontos no tempo usadas nos gráficos. */
public final class TimeSeries {

    private TimeSeries() {
    }

    /**
     * Espalha os pontos pelo período: divide a janela em {@code buckets} faixas de tempo iguais e fica com o último
     * ponto de cada faixa. Assim um gráfico de 24 h cobre as 24 h, e não só os pontos mais recentes. Faixas sem
     * ponto ficam vazias, e o gráfico mostra o buraco.
     *
     * @param sorted pontos em ordem crescente de tempo
     */
    public static <T> List<T> spread(List<T> sorted, Function<T, OffsetDateTime> time,
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
}
