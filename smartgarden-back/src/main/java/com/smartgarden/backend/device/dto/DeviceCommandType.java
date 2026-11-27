package com.smartgarden.backend.device.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeviceCommandType {
    /** Pisca o LED da placa para achar o sensor. */
    @JsonProperty("identify")
    IDENTIFY,
    /** Irrigação simulada: o LED fica aceso pelo tempo informado. */
    @JsonProperty("irrigation")
    IRRIGATION
}
