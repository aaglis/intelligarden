package com.smartgarden.backend.plant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface PlantWaterPointRepository extends JpaRepository<PlantWaterPoint, Long> {

    List<PlantWaterPoint> findByPlantIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(
            Long plantId, OffsetDateTime start, OffsetDateTime end);
}
