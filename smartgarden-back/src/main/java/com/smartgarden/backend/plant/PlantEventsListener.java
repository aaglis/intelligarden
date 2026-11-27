package com.smartgarden.backend.plant;

import com.smartgarden.backend.device.DeviceCommandService;
import com.smartgarden.backend.device.dto.DeviceCommandRequest;
import com.smartgarden.backend.device.dto.DeviceCommandType;
import com.smartgarden.backend.reading.ReadingCreatedEvent;
import com.smartgarden.backend.reading.ReadingEventStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/** Liga as plantas ao resto do sistema: leituras novas, interface (SSE) e LED do sensor (MQTT). */
@Component
public class PlantEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantEventsListener.class);

    private final PlantService plantService;
    private final ReadingEventStreamService eventStream;
    private final DeviceCommandService deviceCommandService;

    public PlantEventsListener(
            PlantService plantService,
            ReadingEventStreamService eventStream,
            DeviceCommandService deviceCommandService
    ) {
        this.plantService = plantService;
        this.eventStream = eventStream;
        this.deviceCommandService = deviceCommandService;
    }

    /** Cada leitura salva atualiza a água estimada das plantas daquele sensor. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReadingCreated(ReadingCreatedEvent event) {
        try {
            plantService.applyReading(event.reading());
        } catch (RuntimeException exception) {
            // A leitura já está salva; a próxima leitura cobre o intervalo inteiro.
            LOGGER.error("Falha ao atualizar a agua estimada com a leitura {}", event.reading().id(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPlantsUpdated(PlantsUpdatedEvent event) {
        eventStream.broadcast(event.deviceCode(), "plants-updated", event.plants());
    }

    /** Irrigação simulada: avisa a interface e acende o LED do sensor pelo tempo proporcional à água. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPlantIrrigated(PlantIrrigatedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plant", event.plant());
        data.put("record", event.record());
        eventStream.broadcast(event.deviceCode(), "plant-irrigated", data);

        if (event.deviceCode() == null) {
            return;
        }
        int seconds = WaterModel.ledSecondsFor(event.record().waterMl());
        try {
            deviceCommandService.send(event.deviceCode(), new DeviceCommandRequest(DeviceCommandType.IRRIGATION, seconds));
        } catch (RuntimeException exception) {
            LOGGER.warn("Irrigacao de {} registrada, mas o LED nao foi acionado: {}",
                    event.plant().code(), exception.getMessage());
        }
    }
}
