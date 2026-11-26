package com.smartgarden.backend.plant.dto;

import com.smartgarden.backend.plant.PlantRecord;
import com.smartgarden.backend.plant.PlantRecordType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

import static com.smartgarden.backend.plant.dto.Numbers.round;

public record PlantRecordResponse(
        Long id,
        @Schema(example = "manjericao")
        String plantCode,
        @Schema(example = "weighing", allowableValues = {"weighing", "watering", "irrigation", "observation"})
        PlantRecordType type,
        OffsetDateTime recordedAt,
        @Schema(description = "Feito pelo sistema (irrigação automática)")
        boolean automatic,
        @Schema(example = "860")
        Double weightGrams,
        @Schema(description = "Pesagem feita logo depois de regar e escorrer")
        Boolean fullPot,
        @Schema(example = "180")
        Integer waterMl,
        @Schema(description = "A água escorreu pelo furo do vaso")
        Boolean drained,
        @Schema(description = "Água estimada logo antes do registro (%)", example = "31.5")
        Double estimatedWaterPercent,
        @Schema(description = "Água medida pela balança (%), quando há peso de vaso cheio", example = "28.0")
        Double measuredWaterPercent,
        @Schema(description = "Perda estimada desde a pesagem anterior (mL)", example = "75")
        Double estimatedLossMl,
        @Schema(description = "Perda medida pela balança desde a pesagem anterior (mL)", example = "80")
        Double measuredLossMl,
        @Schema(description = "k que explicaria a perda medida", example = "3.5")
        Double measuredCoefficient,
        String note
) {

    public static PlantRecordResponse fromEntity(PlantRecord record) {
        return new PlantRecordResponse(
                record.getId(),
                record.getPlant().getPlantCode(),
                record.getType(),
                record.getRecordedAt(),
                record.isAutomatic(),
                record.getWeightG(),
                record.getFullPot(),
                record.getWaterMl(),
                record.getDrained(),
                round(record.getEstimatedWaterPercent(), 1),
                round(record.getMeasuredWaterPercent(), 1),
                round(record.getEstimatedLossMl(), 1),
                round(record.getMeasuredLossMl(), 1),
                round(record.getMeasuredCoefficient(), 3),
                record.getNote()
        );
    }
}
