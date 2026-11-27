package com.smartgarden.backend.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.backend.device.dto.DeviceCommandRequest;
import com.smartgarden.backend.device.dto.DeviceCommandResponse;
import com.smartgarden.backend.device.dto.DeviceCommandType;
import com.smartgarden.backend.device.dto.DeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Envia comandos para o dispositivo pelo tópico smartgarden/devices/{codigo}/commands. */
@Service
public class DeviceCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandService.class);
    static final int DEFAULT_IRRIGATION_SECONDS = 10;

    private final DeviceService deviceService;
    private final ObjectProvider<MqttPahoMessageHandler> mqttOutbound;
    private final ObjectMapper objectMapper;

    public DeviceCommandService(
            DeviceService deviceService,
            ObjectProvider<MqttPahoMessageHandler> mqttOutbound,
            ObjectMapper objectMapper
    ) {
        this.deviceService = deviceService;
        this.mqttOutbound = mqttOutbound;
        this.objectMapper = objectMapper;
    }

    public DeviceCommandResponse send(String deviceCode, DeviceCommandRequest request) {
        DeviceResponse device = deviceService.getDevice(deviceCode);
        MqttPahoMessageHandler outbound = mqttOutbound.getIfAvailable();
        if (outbound == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MQTT desabilitado neste backend.");
        }

        Integer duration = request.command() == DeviceCommandType.IRRIGATION
                ? (request.durationSeconds() != null ? request.durationSeconds() : DEFAULT_IRRIGATION_SECONDS)
                : null;
        UUID commandId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        String topic = "smartgarden/devices/" + device.deviceCode() + "/commands";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commandId", commandId);
        payload.put("command", request.command());
        if (duration != null) {
            payload.put("durationSeconds", duration);
        }
        payload.put("issuedAt", issuedAt);

        try {
            outbound.handleMessage(MessageBuilder.withPayload(objectMapper.writeValueAsString(payload))
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 1)
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao montar o comando", exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao publicar comando MQTT. topico={}", topic, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel enviar o comando ao broker MQTT.");
        }

        LOGGER.info("Comando {} enviado para {} (online={})", request.command(), device.deviceCode(), device.online());
        return new DeviceCommandResponse(commandId, device.deviceCode(), request.command(), duration, topic, issuedAt, device.online());
    }
}
