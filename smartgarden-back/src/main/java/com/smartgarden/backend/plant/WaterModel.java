package com.smartgarden.backend.plant;

/**
 * Contas do modelo de água estimada.
 *
 * <p>VPD (déficit de pressão de vapor) mede o quanto o ar está seco: é a diferença entre o vapor que o ar
 * aguentaria naquela temperatura e o vapor que ele tem. Pressão de saturação pela equação de Tetens,
 * como na FAO-56 (Allen et al., 1998, eq. 11): es = 0,6108 × exp(17,27 T / (T + 237,3)).
 *
 * <p>A perda de água do vaso é aproximada por perda (mL/h) = k × VPD, com k próprio de cada planta e vaso,
 * calibrado comparando com pesagens reais.
 */
public final class WaterModel {

    /** Lacunas maiores que isso (sensor desligado por muito tempo, relógio errado) são limitadas a este valor. */
    public static final double MAX_GAP_HOURS = 12.0;

    private WaterModel() {
    }

    public static double saturationVaporPressureKpa(double temperatureC) {
        return 0.6108 * Math.exp(17.27 * temperatureC / (temperatureC + 237.3));
    }

    public static double vpdKpa(double temperatureC, double humidityPercent) {
        double relativeHumidity = Math.min(Math.max(humidityPercent, 0.0), 100.0) / 100.0;
        return saturationVaporPressureKpa(temperatureC) * (1.0 - relativeHumidity);
    }

    /** Integral do VPD no intervalo (kPa·h), pela média entre a leitura anterior e a atual (trapézio). */
    public static double vpdHours(Double previousVpdKpa, double currentVpdKpa, double hours) {
        double average = previousVpdKpa == null ? currentVpdKpa : (previousVpdKpa + currentVpdKpa) / 2.0;
        return average * Math.min(Math.max(hours, 0.0), MAX_GAP_HOURS);
    }

    /** Segundos de LED aceso para representar a irrigação: 1 s a cada 10 mL, entre 1 e 60 s. */
    public static int ledSecondsFor(int irrigationMl) {
        return (int) Math.min(60, Math.max(1, Math.round(irrigationMl / 10.0)));
    }
}
