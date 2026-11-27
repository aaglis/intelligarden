package com.smartgarden.backend.plant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Registro diário de uma planta: pesagem, rega, irrigação simulada ou observação. */
@Entity
@Table(name = "plant_records")
public class PlantRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 20)
    private PlantRecordType type;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(nullable = false)
    private boolean automatic;

    @Column(name = "weight_g")
    private Double weightG;

    @Column(name = "full_pot")
    private Boolean fullPot;

    @Column(name = "water_ml")
    private Integer waterMl;

    @Column
    private Boolean drained;

    /** Água estimada (%) logo antes do registro. */
    @Column(name = "estimated_water_percent")
    private Double estimatedWaterPercent;

    /** Água medida pela pesagem (%), quando já existe a referência de vaso cheio. */
    @Column(name = "measured_water_percent")
    private Double measuredWaterPercent;

    @Column(name = "estimated_loss_ml")
    private Double estimatedLossMl;

    @Column(name = "measured_loss_ml")
    private Double measuredLossMl;

    @Column(name = "measured_coefficient")
    private Double measuredCoefficient;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static PlantRecord of(Plant plant, PlantRecordType type, OffsetDateTime recordedAt) {
        PlantRecord record = new PlantRecord();
        record.plant = plant;
        record.type = type;
        record.recordedAt = recordedAt;
        record.estimatedWaterPercent = plant.waterPercent();
        return record;
    }

    public Long getId() {
        return id;
    }

    public Plant getPlant() {
        return plant;
    }

    public PlantRecordType getType() {
        return type;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public Double getWeightG() {
        return weightG;
    }

    public void setWeightG(Double weightG) {
        this.weightG = weightG;
    }

    public Boolean getFullPot() {
        return fullPot;
    }

    public void setFullPot(Boolean fullPot) {
        this.fullPot = fullPot;
    }

    public Integer getWaterMl() {
        return waterMl;
    }

    public void setWaterMl(Integer waterMl) {
        this.waterMl = waterMl;
    }

    public Boolean getDrained() {
        return drained;
    }

    public void setDrained(Boolean drained) {
        this.drained = drained;
    }

    public Double getEstimatedWaterPercent() {
        return estimatedWaterPercent;
    }

    public void setEstimatedWaterPercent(Double estimatedWaterPercent) {
        this.estimatedWaterPercent = estimatedWaterPercent;
    }

    public Double getMeasuredWaterPercent() {
        return measuredWaterPercent;
    }

    public void setMeasuredWaterPercent(Double measuredWaterPercent) {
        this.measuredWaterPercent = measuredWaterPercent;
    }

    public Double getEstimatedLossMl() {
        return estimatedLossMl;
    }

    public void setEstimatedLossMl(Double estimatedLossMl) {
        this.estimatedLossMl = estimatedLossMl;
    }

    public Double getMeasuredLossMl() {
        return measuredLossMl;
    }

    public void setMeasuredLossMl(Double measuredLossMl) {
        this.measuredLossMl = measuredLossMl;
    }

    public Double getMeasuredCoefficient() {
        return measuredCoefficient;
    }

    public void setMeasuredCoefficient(Double measuredCoefficient) {
        this.measuredCoefficient = measuredCoefficient;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
