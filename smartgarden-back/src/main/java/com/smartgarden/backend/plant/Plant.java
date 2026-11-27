package com.smartgarden.backend.plant;

import com.smartgarden.backend.device.Device;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Planta do jardim virtual. A água no vaso é estimada (não há sensor de solo): esvazia com o clima medido
 * pelo sensor ligado a ela e enche com regas e irrigações. Pesagens corrigem a estimativa e servem para calibrar k.
 */
@Entity
@Table(name = "plants")
public class Plant {

    /** Perda mínima de clima (kPa·h) entre duas pesagens para a comparação valer para calibrar k. */
    static final double MIN_VPD_HOURS_FOR_CALIBRATION = 3.0;
    /** Intervalo mínimo entre duas irrigações automáticas da mesma planta. */
    static final Duration AUTO_IRRIGATION_COOLDOWN = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plant_code", nullable = false, unique = true, length = 60)
    private String plantCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "scientific_name", length = 160)
    private String scientificName;

    @Column(length = 200)
    private String spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "water_capacity_ml", nullable = false)
    private double waterCapacityMl;

    @Column(name = "irrigation_threshold_percent", nullable = false)
    private double irrigationThresholdPercent;

    @Column(name = "irrigation_ml", nullable = false)
    private int irrigationMl;

    /** k: mL perdidos por hora para cada kPa de VPD. */
    @Column(name = "loss_coefficient", nullable = false)
    private double lossCoefficient;

    /** true: planta virtual, a irrigação simulada enche a caixa d'água. false: vaso real, espera a rega da equipe. */
    @Column(name = "auto_irrigation", nullable = false)
    private boolean autoIrrigation = true;

    @Column(name = "temperature_min_c")
    private Double temperatureMinC;

    @Column(name = "temperature_max_c")
    private Double temperatureMaxC;

    @Column(name = "humidity_min_percent")
    private Double humidityMinPercent;

    @Column(name = "humidity_max_percent")
    private Double humidityMaxPercent;

    @Column(name = "estimated_water_ml", nullable = false)
    private double estimatedWaterMl;

    @Column(name = "estimated_at", nullable = false)
    private OffsetDateTime estimatedAt;

    @Column(name = "last_vpd_kpa")
    private Double lastVpdKpa;

    @Column(name = "needs_water", nullable = false)
    private boolean needsWater;

    @Column(name = "last_watered_at")
    private OffsetDateTime lastWateredAt;

    @Column(name = "full_weight_g")
    private Double fullWeightG;

    @Column(name = "last_weight_g")
    private Double lastWeightG;

    @Column(name = "last_weighed_at")
    private OffsetDateTime lastWeighedAt;

    @Column(name = "estimated_loss_since_weighing_ml", nullable = false)
    private double estimatedLossSinceWeighingMl;

    @Column(name = "vpd_hours_since_weighing", nullable = false)
    private double vpdHoursSinceWeighing;

    @Column(name = "watered_since_weighing", nullable = false)
    private boolean wateredSinceWeighing;

    @Column(name = "calibration_samples", nullable = false)
    private int calibrationSamples;

    @Column(name = "suggested_coefficient")
    private Double suggestedCoefficient;

    @Column(name = "mean_error_percent")
    private Double meanErrorPercent;

    /** Hora do último ponto guardado na curva da água (plant_water_history). */
    @Column(name = "water_history_at")
    private OffsetDateTime waterHistoryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ---------------------------------------------------------------- modelo de água

    public double waterPercent() {
        return waterCapacityMl <= 0 ? 0.0 : 100.0 * estimatedWaterMl / waterCapacityMl;
    }

    /** Ritmo atual de perda (mL/h) com o último VPD medido; null se ainda não chegou leitura. */
    public Double currentLossMlPerHour() {
        return lastVpdKpa == null ? null : lossCoefficient * lastVpdKpa;
    }

    /** Horas até chegar no limite mantendo o clima atual; 0 se já chegou, null se não dá para prever. */
    public Double hoursToThreshold() {
        Double rate = currentLossMlPerHour();
        double thresholdMl = waterCapacityMl * irrigationThresholdPercent / 100.0;
        if (estimatedWaterMl <= thresholdMl) {
            return 0.0;
        }
        if (rate == null || rate <= 0) {
            return null;
        }
        return (estimatedWaterMl - thresholdMl) / rate;
    }

    /**
     * Desconta a água perdida desde a última leitura usando o VPD da leitura nova.
     * A primeira leitura só ancora a estimativa (horário e VPD); leituras fora de ordem são ignoradas.
     *
     * @return mL perdidos no intervalo
     */
    public double applyClimate(double vpdKpa, OffsetDateTime at) {
        if (lastVpdKpa == null || estimatedAt == null) {
            estimatedAt = at;
            lastVpdKpa = vpdKpa;
            refreshNeedsWater();
            return 0.0;
        }
        if (!at.isAfter(estimatedAt)) {
            return 0.0;
        }
        double hours = Duration.between(estimatedAt, at).toMillis() / 3_600_000.0;
        double vpdHours = WaterModel.vpdHours(lastVpdKpa, vpdKpa, hours);
        double loss = Math.min(estimatedWaterMl, lossCoefficient * vpdHours);

        estimatedWaterMl -= loss;
        estimatedLossSinceWeighingMl += loss;
        vpdHoursSinceWeighing += vpdHours;
        estimatedAt = at;
        lastVpdKpa = vpdKpa;
        refreshNeedsWater();
        return loss;
    }

    /** Rega da equipe ou irrigação simulada. Rega escorrida (saiu água pelo furo) deixa o vaso cheio. */
    public void addWater(double waterMl, boolean saturate, OffsetDateTime at) {
        estimatedWaterMl = saturate ? waterCapacityMl : Math.min(waterCapacityMl, estimatedWaterMl + waterMl);
        wateredSinceWeighing = true;
        lastWateredAt = at;
        refreshNeedsWater();
    }

    public boolean canAutoIrrigate(OffsetDateTime now) {
        return active
                && autoIrrigation
                && needsWater
                && (lastWateredAt == null || !lastWateredAt.plus(AUTO_IRRIGATION_COOLDOWN).isAfter(now));
    }

    /**
     * Registra uma pesagem: compara a perda medida com a estimada desde a pesagem anterior e corrige a estimativa.
     *
     * @param fullPot pesado logo depois de regar e escorrer; vira a referência de vaso cheio
     */
    public WeighingResult registerWeighing(double weightG, boolean fullPot, OffsetDateTime at) {
        double estimatedPercentBefore = waterPercent();
        Double estimatedLoss = null;
        Double measuredLoss = null;
        Double measuredCoefficient = null;
        if (!fullPot && lastWeightG != null && !wateredSinceWeighing) {
            estimatedLoss = estimatedLossSinceWeighingMl;
            measuredLoss = lastWeightG - weightG;
            if (measuredLoss > 0 && vpdHoursSinceWeighing >= MIN_VPD_HOURS_FOR_CALIBRATION) {
                measuredCoefficient = measuredLoss / vpdHoursSinceWeighing;
            }
        }

        Double measuredPercent = null;
        if (fullPot) {
            fullWeightG = weightG;
            estimatedWaterMl = waterCapacityMl;
            measuredPercent = 100.0;
        } else if (fullWeightG != null) {
            // 1 g = 1 mL de água; o que falta para o peso de vaso cheio é a água que saiu.
            estimatedWaterMl = Math.min(waterCapacityMl, Math.max(0.0, waterCapacityMl - (fullWeightG - weightG)));
            measuredPercent = waterPercent();
        }

        lastWeightG = weightG;
        lastWeighedAt = at;
        estimatedLossSinceWeighingMl = 0.0;
        vpdHoursSinceWeighing = 0.0;
        wateredSinceWeighing = false;
        refreshNeedsWater();
        return new WeighingResult(estimatedPercentBefore, measuredPercent, estimatedLoss, measuredLoss, measuredCoefficient);
    }

    public void updateCalibration(int samples, Double suggested, Double meanError) {
        this.calibrationSamples = samples;
        this.suggestedCoefficient = suggested;
        this.meanErrorPercent = meanError;
    }

    public void refreshNeedsWater() {
        needsWater = waterPercent() <= irrigationThresholdPercent;
    }

    /** Troca a capacidade do vaso mantendo a mesma porcentagem de água estimada. */
    public void changeCapacityKeepingPercent(double newCapacityMl) {
        double percent = waterPercent();
        waterCapacityMl = newCapacityMl;
        estimatedWaterMl = newCapacityMl * percent / 100.0;
        refreshNeedsWater();
    }

    /** Começa a estimativa com o vaso na porcentagem informada. */
    public void startEstimate(double waterPercent, OffsetDateTime at) {
        estimatedWaterMl = waterCapacityMl * Math.min(Math.max(waterPercent, 0.0), 100.0) / 100.0;
        estimatedAt = at;
        refreshNeedsWater();
    }

    public record WeighingResult(
            double estimatedPercentBefore,
            Double measuredPercent,
            Double estimatedLossMl,
            Double measuredLossMl,
            Double measuredCoefficient
    ) {
    }

    // ---------------------------------------------------------------- acesso

    public Long getId() {
        return id;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getSpot() {
        return spot;
    }

    public void setSpot(String spot) {
        this.spot = spot;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getWaterCapacityMl() {
        return waterCapacityMl;
    }

    public void setWaterCapacityMl(double waterCapacityMl) {
        this.waterCapacityMl = waterCapacityMl;
    }

    public double getIrrigationThresholdPercent() {
        return irrigationThresholdPercent;
    }

    public void setIrrigationThresholdPercent(double irrigationThresholdPercent) {
        this.irrigationThresholdPercent = irrigationThresholdPercent;
    }

    public int getIrrigationMl() {
        return irrigationMl;
    }

    public void setIrrigationMl(int irrigationMl) {
        this.irrigationMl = irrigationMl;
    }

    public double getLossCoefficient() {
        return lossCoefficient;
    }

    public void setLossCoefficient(double lossCoefficient) {
        this.lossCoefficient = lossCoefficient;
    }

    public boolean isAutoIrrigation() {
        return autoIrrigation;
    }

    public void setAutoIrrigation(boolean autoIrrigation) {
        this.autoIrrigation = autoIrrigation;
    }

    public Double getTemperatureMinC() {
        return temperatureMinC;
    }

    public void setTemperatureMinC(Double temperatureMinC) {
        this.temperatureMinC = temperatureMinC;
    }

    public Double getTemperatureMaxC() {
        return temperatureMaxC;
    }

    public void setTemperatureMaxC(Double temperatureMaxC) {
        this.temperatureMaxC = temperatureMaxC;
    }

    public Double getHumidityMinPercent() {
        return humidityMinPercent;
    }

    public void setHumidityMinPercent(Double humidityMinPercent) {
        this.humidityMinPercent = humidityMinPercent;
    }

    public Double getHumidityMaxPercent() {
        return humidityMaxPercent;
    }

    public void setHumidityMaxPercent(Double humidityMaxPercent) {
        this.humidityMaxPercent = humidityMaxPercent;
    }

    public double getEstimatedWaterMl() {
        return estimatedWaterMl;
    }

    public OffsetDateTime getEstimatedAt() {
        return estimatedAt;
    }

    public Double getLastVpdKpa() {
        return lastVpdKpa;
    }

    public boolean isNeedsWater() {
        return needsWater;
    }

    public OffsetDateTime getLastWateredAt() {
        return lastWateredAt;
    }

    public Double getFullWeightG() {
        return fullWeightG;
    }

    public Double getLastWeightG() {
        return lastWeightG;
    }

    public OffsetDateTime getLastWeighedAt() {
        return lastWeighedAt;
    }

    public double getEstimatedLossSinceWeighingMl() {
        return estimatedLossSinceWeighingMl;
    }

    public double getVpdHoursSinceWeighing() {
        return vpdHoursSinceWeighing;
    }

    public boolean isWateredSinceWeighing() {
        return wateredSinceWeighing;
    }

    public int getCalibrationSamples() {
        return calibrationSamples;
    }

    public Double getSuggestedCoefficient() {
        return suggestedCoefficient;
    }

    public Double getMeanErrorPercent() {
        return meanErrorPercent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.estimatedAt == null) {
            this.estimatedAt = now;
        }
    }

    public OffsetDateTime getWaterHistoryAt() {
        return waterHistoryAt;
    }

    public void setWaterHistoryAt(OffsetDateTime waterHistoryAt) {
        this.waterHistoryAt = waterHistoryAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
