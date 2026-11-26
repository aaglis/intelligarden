package com.smartgarden.backend.device.dto;

import com.smartgarden.backend.device.Device;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record DeviceResponse(
        @Schema(example = "1")
        Long id,
        @Schema(example = "esp32-jardim-bloco-a")
        String deviceCode,
        @Schema(example = "ESP32 Jardim Bloco A")
        String name,
        @Schema(example = "Universidade - Bloco A")
        String location,
        @Schema(example = "true")
        boolean active,
        @Schema(example = "2026-05-21T12:00:00Z")
        OffsetDateTime createdAt,
        @Schema(example = "2026-05-21T12:16:53Z")
        OffsetDateTime updatedAt,
        @Schema(example = "2026-05-21T12:16:53Z")
        OffsetDateTime lastSeenAt,
        @Schema(description = "Conectado ao broker MQTT agora (informado pelo próprio dispositivo)", example = "true")
        boolean online,
        @Schema(description = "Quando o estado online/offline mudou pela última vez", example = "2026-05-21T12:16:53Z")
        OffsetDateTime statusChangedAt,
        @Schema(example = "10.50.22.73")
        String ipAddress,
        @Schema(example = "1.1.0")
        String firmwareVersion
) {

    public static DeviceResponse fromEntity(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceCode(),
                device.getName(),
                device.getLocation(),
                device.isActive(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.getLastSeenAt(),
                device.isOnline(),
                device.getStatusChangedAt(),
                device.getIpAddress(),
                device.getFirmwareVersion()
        );
    }
}
