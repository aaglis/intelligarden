package com.smartgarden.backend.plant;

import com.smartgarden.backend.plant.dto.PlantRecordResponse;
import com.smartgarden.backend.plant.dto.PlantResponse;

/** Irrigação simulada disparada: acende o LED do sensor e avisa a interface ("plant-irrigated"). */
public record PlantIrrigatedEvent(String deviceCode, PlantResponse plant, PlantRecordResponse record) {
}
