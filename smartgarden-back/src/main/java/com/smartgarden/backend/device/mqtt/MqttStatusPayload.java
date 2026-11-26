package com.smartgarden.backend.device.mqtt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mensagem retida em smartgarden/devices/{codigo}/status.
 * O ESP32 publica "online" ao conectar; o broker publica "offline" sozinho (last will) se ele sumir.
 */
public record MqttStatusPayload(
        @NotNull @Pattern(regexp = "online|offline") String state,
        @Size(max = 45) String ip,
        @Size(max = 30) String firmware
) {

    public boolean online() {
        return "online".equals(state);
    }
}
