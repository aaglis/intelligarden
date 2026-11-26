package com.smartgarden.backend.device.mqtt;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartgarden.backend.device.DeviceService;
import com.smartgarden.backend.device.dto.DeviceResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MqttStatusHandlerTest {

    private static final String TOPIC = "smartgarden/devices/esp32-jardim/status";

    private DeviceService deviceService;
    private MqttStatusHandler handler;

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        when(deviceService.updateStatus(any(), anyBoolean(), any(), any())).thenReturn(
                new DeviceResponse(1L, "esp32-jardim", "ESP32", null, true, null, null, null, true, null, null, null));
        handler = new MqttStatusHandler(
                JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                deviceService
        );
    }

    @Test
    void shouldMarkDeviceOnlineWithIpAndFirmware() {
        handle(TOPIC, "{\"state\":\"online\",\"ip\":\"10.50.22.73\",\"firmware\":\"1.1.0\"}");

        verify(deviceService).updateStatus("esp32-jardim", true, "10.50.22.73", "1.1.0");
    }

    @Test
    void shouldMarkDeviceOfflineFromLastWill() {
        handle(TOPIC, "{\"state\":\"offline\"}");

        verify(deviceService).updateStatus(eq("esp32-jardim"), eq(false), isNull(), isNull());
    }

    @Test
    void shouldIgnoreClearedRetainedMessage() {
        handle(TOPIC, "");

        verifyNoInteractions(deviceService);
    }

    @Test
    void shouldDiscardUnknownState() {
        handle(TOPIC, "{\"state\":\"sleeping\"}");

        verifyNoInteractions(deviceService);
    }

    @Test
    void shouldDiscardInvalidTopic() {
        handle("smartgarden/devices/esp32-jardim/telemetry", "{\"state\":\"online\"}");

        verifyNoInteractions(deviceService);
    }

    private void handle(String topic, String payload) {
        handler.handle(MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
                .build());
    }
}
