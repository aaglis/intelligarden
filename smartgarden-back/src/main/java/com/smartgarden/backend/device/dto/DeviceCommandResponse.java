package com.smartgarden.backend.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceCommandResponse(
        UUID commandId,
        @Schema(example = "esp32-jardim-bloco-a")
        String deviceCode,
        DeviceCommandType command,
        @Schema(example = "10")
        Integer durationSeconds,
        @Schema(example = "smartgarden/devices/esp32-jardim-bloco-a/commands")
        String topic,
        OffsetDateTime issuedAt,
        @Schema(description = "Se o dispositivo estava online no envio. Offline: o comando é descartado pelo broker.", example = "true")
        boolean deviceOnline
) {
}
