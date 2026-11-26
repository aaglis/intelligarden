package com.smartgarden.backend.config.mqtt;

import com.smartgarden.backend.device.mqtt.MqttStatusHandler;
import com.smartgarden.backend.reading.mqtt.MqttTelemetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** Entrega cada mensagem MQTT recebida ao handler do tipo de tópico (telemetria ou status). */
@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttInboundDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttInboundDispatcher.class);

    private final MqttTelemetryHandler telemetryHandler;
    private final MqttStatusHandler statusHandler;

    public MqttInboundDispatcher(MqttTelemetryHandler telemetryHandler, MqttStatusHandler statusHandler) {
        this.telemetryHandler = telemetryHandler;
        this.statusHandler = statusHandler;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void dispatch(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        if (topic != null && topic.endsWith("/telemetry")) {
            telemetryHandler.handle(message);
        } else if (topic != null && topic.endsWith("/status")) {
            statusHandler.handle(message);
        } else {
            LOGGER.warn("Mensagem MQTT ignorada: topico sem handler. topico={}", topic);
        }
    }
}
