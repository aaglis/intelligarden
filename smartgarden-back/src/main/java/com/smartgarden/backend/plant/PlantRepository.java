package com.smartgarden.backend.plant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /*
     * As travas são "for update" escritas na própria consulta. Com @Lock, o Hibernate no PostgreSQL lia a linha
     * e só travava depois, num segundo select (follow-on locking), e o estado lido podia já estar velho.
     */

    /** Trava a linha: leituras do sensor e registros da equipe mexem na mesma caixa d'água. */
    @Query(value = "select * from plants where plant_code = :plantCode for update", nativeQuery = true)
    Optional<Plant> findWithLockByPlantCode(@Param("plantCode") String plantCode);

    /** Plantas ativas do sensor, travadas e sempre na mesma ordem (evita impasse entre transações). */
    @Query(value = "select * from plants where device_id = :deviceId and active = true order by id for update",
            nativeQuery = true)
    List<Plant> findWithLockByDeviceIdAndActiveTrueOrderByIdAsc(@Param("deviceId") Long deviceId);
}
