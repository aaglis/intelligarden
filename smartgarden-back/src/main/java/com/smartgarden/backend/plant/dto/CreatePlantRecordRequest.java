package com.smartgarden.backend.plant.dto;

import com.smartgarden.backend.plant.PlantRecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlantRecordRequest(
        @Schema(example = "weighing", allowableValues = {"weighing", "watering", "observation"})
        @NotNull
        PlantRecordType type,
        @Schema(description = "weighing: peso do vaso sem o pratinho, em gramas", example = "860")
        @DecimalMin("1") @DecimalMax("50000")
        Double weightGrams,
        @Schema(description = "weighing: pesado logo depois de regar e escorrer (vira a referência de vaso cheio)")
        Boolean fullPot,
        @Schema(description = "watering: água colocada, em mL", example = "180")
        @Min(1) @Max(5000)
        Integer waterMl,
        @Schema(description = "watering: a água escorreu pelo furo (o vaso ficou cheio)")
        Boolean drained,
        @Schema(description = "Observação livre", example = "Folhas de baixo amareladas")
        @Size(max = 500)
        String note
) {
}
