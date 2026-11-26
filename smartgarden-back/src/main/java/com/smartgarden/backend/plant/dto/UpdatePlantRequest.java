package com.smartgarden.backend.plant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Atualização parcial: campos nulos ficam como estão. */
public record UpdatePlantRequest(
        @Size(min = 1, max = 120)
        String name,
        @Size(max = 160)
        String scientificName,
        @Size(max = 200)
        String spot,
        String deviceCode,
        Boolean active,
        @DecimalMin("10") @DecimalMax("20000")
        Double waterCapacityMl,
        @DecimalMin("0") @DecimalMax("95")
        Double irrigationThresholdPercent,
        @Min(1) @Max(20000)
        Integer irrigationMl,
        @Schema(description = "k: mL por hora por kPa de VPD. Use o suggestedCoefficient depois de algumas pesagens")
        @DecimalMin("0") @DecimalMax("100")
        Double lossCoefficient,
        Boolean autoIrrigation,
        Double temperatureMinC,
        Double temperatureMaxC,
        @DecimalMin("0") @DecimalMax("100")
        Double humidityMinPercent,
        @DecimalMin("0") @DecimalMax("100")
        Double humidityMaxPercent
) {
}
