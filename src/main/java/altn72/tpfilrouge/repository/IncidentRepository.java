package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByEquipementIdOrderByDateHeureOuvertureDesc(Long equipementId);

    List<Incident> findByStatutOrderByDateHeureResolutionDesc(StatutIncident statut);

    // Les « join fetch » évitent une requête par ligne pour afficher la station, l'équipement et le prestataire.
    @Query("""
            select i from Incident i
            join fetch i.equipement e
            join fetch e.station s
            left join fetch i.prestataire
            where i.statut <> altn72.tpfilrouge.modele.StatutIncident.RESOLU
            and (:stationId is null or s.id = :stationId)
            and (:type is null or e.type = :type)
            """)
    List<Incident> trouverIncidentsActifs(@Param("stationId") Long stationId, @Param("type") String type);
}