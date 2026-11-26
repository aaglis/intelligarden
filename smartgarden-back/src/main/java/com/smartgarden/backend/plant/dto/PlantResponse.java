package com.smartgarden.backend.plant.dto;

import com.smartgarden.backend.plant.Plant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

import static com.smartgarden.backend.plant.dto.Numbers.round;

public record PlantResponse(
        @Schema(example = "manjericao")
        String code,
        @Schema(example = "Manjericão")
        String name,
        @Schema(example = "Ocimum basilicum")
        String scientificName,
        @Schema(example = "No parapeito da janela · sol direto")
        String spot,
        @Schema(description = "Sensor que mede o clima desta planta", example = "esp32-jardim-bloco-a")
        String deviceCode,
        boolean active,
        @Schema(description = "true: irrigação simulada automática no limite. false: vaso real, espera a rega da equipe")
        boolean autoIrrigation,

        @Schema(description = "Água que o vaso guarda entre cheio (regado e escorrido) e seco", example = "260")
        double waterCapacityMl,
        @Schema(description = "Abaixo desta porcentagem a planta precisa de água", example = "30")
        double irrigationThresholdPercent,
        @Schema(description = "Quanto a irrigação/rega recomendada coloca", example = "180")
        int irrigationMl,
        @Schema(description = "k: mL perdidos por hora para cada kPa de VPD", example = "3.3")
        double lossCoefficient,
        Double temperatureMinC,
        Double temperatureMaxC,
        Double humidityMinPercent,
        Double humidityMaxPercent,

        @Schema(description = "Água estimada no vaso agora (mL)", example = "120.4")
        double estimatedWaterMl,
        @Schema(description = "Água estimada no vaso agora (% da capacidade)", example = "46.3")
        double estimatedWaterPercent,
        @Schema(description = "Horário da última leitura usada na estimativa")
        OffsetDateTime estimatedAt,
        @Schema(description = "VPD da última leitura do sensor (kPa)", example = "1.79")
        Double lastVpdKpa,
        @Schema(description = "Ritmo de perda com o clima atual (mL/h)", example = "5.9")
        Double lossMlPerHour,
        @Schema(description = "Horas até o limite se o clima continuar igual; 0 se já chegou", example = "7.2")
        Double hoursToThreshold,
        boolean needsWater,
        OffsetDateTime lastWateredAt,

        @Schema(description = "Peso do vaso cheio (referência para medir a água pela balança)", example = "1040")
        Double fullWeightG,
        Double lastWeightG,
        OffsetDateTime lastWeighedAt,
        @Schema(description = "Perda estimada desde a última pesagem (mL)", example = "35.2")
        double estimatedLossSinceWeighingMl,
        @Schema(description = "Pesagens com comparação medida × estimada usadas na calibração", example = "4")
        int calibrationSamples,
        @Schema(description = "k sugerido pelas pesagens (mediana das últimas 10)", example = "3.1")
        Double suggestedCoefficient,
        @Schema(description = "Erro médio da estimativa nessas pesagens (%)", example = "12.5")
        Double meanErrorPercent
) {

    public static PlantResponse fromEntity(Plant plant) {
        return new PlantResponse(
                plant.getPlantCode(),
                plant.getName(),
                plant.getScientificName(),
                plant.getSpot(),
                plant.getDevice() == null ? null : plant.getDevice().getDeviceCode(),
                plant.isActive(),
                plant.isAutoIrrigation(),
                plant.getWaterCapacityMl(),
                plant.getIrrigationThresholdPercent(),
                plant.getIrrigationMl(),
                plant.getLossCoefficient(),
                plant.getTemperatureMinC(),
                plant.getTemperatureMaxC(),
                plant.getHumidityMinPercent(),
                plant.getHumidityMaxPercent(),
                round(plant.getEstimatedWaterMl(), 1),
                round(plant.waterPercent(), 1),
                plant.getEstimatedAt(),
                round(plant.getLastVpdKpa(), 3),
                round(plant.currentLossMlPerHour(), 2),
                round(plant.hoursToThreshold(), 1),
                plant.isNeedsWater(),
                plant.getLastWateredAt(),
                plant.getFullWeightG(),
                plant.getLastWeightG(),
                plant.getLastWeighedAt(),
                round(plant.getEstimatedLossSinceWeighingMl(), 1),
                plant.getCalibrationSamples(),
                round(plant.getSuggestedCoefficient(), 3),
                round(plant.getMeanErrorPercent(), 1)
        );
    }
}
