package com.smartgarden.backend.plant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Um ponto da curva da água estimada de uma planta (veja a migração V8). */
@Entity
@Table(name = "plant_water_history")
public class PlantWaterPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "water_ml", nullable = false)
    private double waterMl;

    @Column(name = "water_percent", nullable = false)
    private double waterPercent;

    protected PlantWaterPoint() {
    }

    static PlantWaterPoint of(Plant plant, OffsetDateTime at) {
        PlantWaterPoint point = new PlantWaterPoint();
        point.plant = plant;
        point.recordedAt = at;
        point.waterMl = plant.getEstimatedWaterMl();
        point.waterPercent = plant.waterPercent();
        return point;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public double getWaterMl() {
        return waterMl;
    }

    public double getWaterPercent() {
        return waterPercent;
    }
}
