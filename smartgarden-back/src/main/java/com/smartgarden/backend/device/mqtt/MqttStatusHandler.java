package com.smartgarden.backend.device.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.backend.device.DeviceService;
import com.smartgarden.backend.device.dto.DeviceResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttStatusHandler.class);
    private static final Pattern STATUS_TOPIC = Pattern.compile("^smartgarden/devices/([^/]{1,100})/status$");

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final DeviceService deviceService;

    public MqttStatusHandler(ObjectMapper objectMapper, Validator validator, DeviceService deviceService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.deviceService = deviceService;
    }

    public void handle(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = message.getPayload() instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8)
                : message.getPayload().toString();

        // Mensagem vazia = alguém limpou o status retido; não indica online nem offline.
        if (payload.isBlank()) {
            return;
        }

        try {
            String deviceCode = extractDeviceCode(topic);
            MqttStatusPayload status = objectMapper.readValue(payload, MqttStatusPayload.class);
            validate(status);
            DeviceResponse device = deviceService.updateStatus(deviceCode, status.online(), status.ip(), status.firmware());
            LOGGER.info("Dispositivo {} {}", device.deviceCode(), device.online() ? "online" : "offline");
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            LOGGER.warn("Mensagem MQTT de status descartada. topico={}, motivo={}", topic, exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao processar status MQTT. topico={}", topic, exception);
        }
    }

    private String extractDeviceCode(String topic) {
        if (topic == null) {
            throw new IllegalArgumentException("Topico MQTT ausente");
        }
        Matcher matcher = STATUS_TOPIC.matcher(topic);
        if (!matcher.matches() || matcher.group(1).isBlank()) {
            throw new IllegalArgumentException("Topico MQTT invalido");
        }
        return matcher.group(1);
    }

    private void validate(MqttStatusPayload status) {
        Set<ConstraintViolation<MqttStatusPayload>> violations = validator.validate(status);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(details);
        }
    }
}
