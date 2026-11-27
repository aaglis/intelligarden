package com.smartgarden.backend.plant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlantRequest(
        @Schema(description = "Identificador na URL (minúsculas, números e hífen)", example = "alecrim")
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,60}$")
        String code,
        @Schema(example = "Alecrim")
        @NotBlank @Size(max = 120)
        String name,
        @Schema(example = "Salvia rosmarinus")
        @Size(max = 160)
        String scientificName,
        @Schema(example = "No parapeito da janela · sol direto")
        @Size(max = 200)
        String spot,
        @Schema(description = "Sensor que mede o clima da planta", example = "esp32-jardim-bloco-a")
        @NotBlank
        String deviceCode,
        @Schema(example = "250")
        @NotNull @DecimalMin("10") @DecimalMax("20000")
        Double waterCapacityMl,
        @Schema(example = "30")
        @NotNull @DecimalMin("0") @DecimalMax("95")
        Double irrigationThresholdPercent,
        @Schema(description = "Padrão: o que leva do limite ao vaso cheio", example = "175")
        @Min(1) @Max(20000)
        Integer irrigationMl,
        @Schema(description = "k: mL por hora por kPa de VPD", example = "2.0")
        @NotNull @DecimalMin("0") @DecimalMax("100")
        Double lossCoefficient,
        @Schema(description = "Padrão true (planta virtual)")
        Boolean autoIrrigation,
        Double temperatureMinC,
        Double temperatureMaxC,
        @DecimalMin("0") @DecimalMax("100")
        Double humidityMinPercent,
        @DecimalMin("0") @DecimalMax("100")
        Double humidityMaxPercent,
        @Schema(description = "Água no vaso ao começar (%), padrão 100", example = "100")
        @DecimalMin("0") @DecimalMax("100")
        Double initialWaterPercent
) {
}
