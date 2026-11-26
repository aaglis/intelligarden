package com.smartgarden.backend.plant;

import com.smartgarden.backend.plant.dto.PlantResponse;

import java.util.List;

/** Água estimada ou dados de plantas mudaram (vai para a interface como evento SSE "plants-updated"). */
public record PlantsUpdatedEvent(String deviceCode, List<PlantResponse> plants) {
}
