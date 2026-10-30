package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatutOrderByDateHeureResolutionDesc(String statut);

    List<Incident> findByEquipementIdOrderByDateHeureOuvertureDesc(Long equipementId);

    @Query("SELECT i FROM Incident i WHERE i.statut != 'RESOLU'")
    List<Incident> trouverIncidentsActifs();

    @Query(value = "SELECT COUNT(*) FROM incident i " +
            "JOIN equipement e ON i.equipement_id = e.id " +
            "WHERE e.station_id = :stationId",
            nativeQuery = true)
    long compterIncidentsParStationNative(@Param("stationId") Long stationId);
}