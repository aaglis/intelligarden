package com.smartgarden.backend.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartgarden.backend.device.dto.DeviceCommandRequest;
import com.smartgarden.backend.device.dto.DeviceCommandType;
import com.smartgarden.backend.device.dto.DeviceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceCommandServiceTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private DeviceService deviceService;
    private MqttPahoMessageHandler outbound;
    private ObjectProvider<MqttPahoMessageHandler> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        deviceService = mock(DeviceService.class);
        outbound = mock(MqttPahoMessageHandler.class);
        provider = mock(ObjectProvider.class);
        when(deviceService.getDevice("esp32-jardim")).thenReturn(
                new DeviceResponse(1L, "esp32-jardim", "ESP32", null, true, null, null, null, true, null, null, null));
    }

    @Test
    void shouldPublishIrrigationWithDefaultDurationToDeviceTopic() throws Exception {
        when(provider.getIfAvailable()).thenReturn(outbound);
        var service = new DeviceCommandService(deviceService, provider, objectMapper);

        var response = service.send("esp32-jardim", new DeviceCommandRequest(DeviceCommandType.IRRIGATION, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(outbound).handleMessage(captor.capture());
        Message<?> message = captor.getValue();
        assertThat(message.getHeaders().get(MqttHeaders.TOPIC)).isEqualTo("smartgarden/devices/esp32-jardim/commands");
        var json = objectMapper.readTree(message.getPayload().toString());
        assertThat(json.get("command").asText()).isEqualTo("irrigation");
        assertThat(json.get("durationSeconds").asInt()).isEqualTo(10);
        assertThat(response.durationSeconds()).isEqualTo(10);
        assertThat(response.deviceOnline()).isTrue();
    }

    @Test
    void shouldSendIdentifyWithoutDuration() throws Exception {
        when(provider.getIfAvailable()).thenReturn(outbound);
        var service = new DeviceCommandService(deviceService, provider, objectMapper);

        service.send("esp32-jardim", new DeviceCommandRequest(DeviceCommandType.IDENTIFY, 30));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(outbound).handleMessage(captor.capture());
        var json = objectMapper.readTree(captor.getValue().getPayload().toString());
        assertThat(json.get("command").asText()).isEqualTo("identify");
        assertThat(json.has("durationSeconds")).isFalse();
    }

    @Test
    void shouldAnswerServiceUnavailableWhenMqttIsDisabled() {
        when(provider.getIfAvailable()).thenReturn(null);
        var service = new DeviceCommandService(deviceService, provider, objectMapper);

        assertThatThrownBy(() -> service.send("esp32-jardim", new DeviceCommandRequest(DeviceCommandType.IDENTIFY, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
