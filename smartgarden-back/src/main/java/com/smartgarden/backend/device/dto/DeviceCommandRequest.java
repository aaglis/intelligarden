package com.smartgarden.backend.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeviceCommandRequest(
        @Schema(example = "irrigation", allowableValues = {"identify", "irrigation"})
        @NotNull
        DeviceCommandType command,
        @Schema(description = "Só para irrigation: por quantos segundos o LED fica aceso (padrão 10)", example = "10")
        @Min(1) @Max(60)
        Integer durationSeconds
) {
}
