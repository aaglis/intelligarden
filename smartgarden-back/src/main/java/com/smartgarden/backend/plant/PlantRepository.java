package com.smartgarden.backend.plant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {

    @EntityGraph(attributePaths = "device")
    List<Plant> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "device")
    List<Plant> findByDeviceDeviceCodeOrderByIdAsc(String deviceCode);

    @EntityGraph(attributePaths = "device")
    Optional<Plant> findByPlantCode(String plantCode);

    boolean existsByPlantCode(String plantCode);

    /** Trava a linha: leituras do sensor e registros da equipe mexem na mesma caixa d'água. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Plant> findWithLockByPlantCode(String plantCode);

    /** Plantas ativas do sensor, travadas e sempre na mesma ordem (evita impasse entre transações). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Plant> findWithLockByDeviceIdAndActiveTrueOrderByIdAsc(Long deviceId);
}
