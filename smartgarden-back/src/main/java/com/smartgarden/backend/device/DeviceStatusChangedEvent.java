package com.smartgarden.backend.device;

import com.smartgarden.backend.device.dto.DeviceResponse;

/** Publicado quando um dispositivo fica online ou offline (via MQTT). */
public record DeviceStatusChangedEvent(DeviceResponse device) {
}
