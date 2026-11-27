package com.smartgarden.backend.plant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Curva da água estimada de uma planta no período, para o gráfico da ficha")
public record PlantWaterHistoryResponse(
        @Schema(example = "manjericao")
        String plantCode,
        @Schema(description = "Capacidade do vaso em mL (100 %)", example = "260")
        double waterCapacityMl,
        @Schema(description = "Limite de irrigação em %", example = "30")
        double irrigationThresholdPercent,
        OffsetDateTime windowStart,
        OffsetDateTime windowEnd,
        List<Point> points
) {
    public record Point(
            OffsetDateTime recordedAt,
            @Schema(example = "142.5")
            double waterMl,
            @Schema(example = "54.8")
            double waterPercent
    ) {
    }
}
