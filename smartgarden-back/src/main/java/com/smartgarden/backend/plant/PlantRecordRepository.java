package com.smartgarden.backend.plant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantRecordRepository extends JpaRepository<PlantRecord, Long> {

    List<PlantRecord> findByPlantIdOrderByRecordedAtDescIdDesc(Long plantId, Pageable pageable);

    /** Últimas pesagens que tiveram comparação medida × estimada (para calibrar k). */
    List<PlantRecord> findByPlantIdAndTypeAndMeasuredLossMlIsNotNullOrderByRecordedAtDesc(
            Long plantId, PlantRecordType type, Pageable pageable);
}
