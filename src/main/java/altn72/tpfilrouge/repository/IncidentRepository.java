package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.IncidentsParStation;
import altn72.tpfilrouge.modele.NombreIncidentsParType;
import altn72.tpfilrouge.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByEquipementIdOrderByDateHeureOuvertureDesc(Long equipementId);

    List<Incident> findByStatutOrderByDateHeureResolutionDesc(StatutIncident statut);

    List<Incident> findByDateHeureOuvertureGreaterThanEqual(LocalDateTime debut);

    @Query("""
            select new altn72.tpfilrouge.modele.NombreIncidentsParType(e.type, count(i))
            from Incident i
            join i.equipement e
            group by e.type
            order by count(i) desc, e.type
            """)
    List<NombreIncidentsParType> compterIncidentsParType();

    // Seule requête SQL native du projet : les jointures externes partent de la station pour garder celles sans incident.
    @Query(value = """
            SELECT s.nom AS station,
                   COUNT(CASE WHEN i.statut <> 'RESOLU' THEN 1 END) AS nombreActifs,
                   COUNT(CASE WHEN i.statut = 'RESOLU' THEN 1 END) AS nombreResolus
            FROM station s
            LEFT JOIN equipement e ON e.id_station = s.id_station
            LEFT JOIN incident i ON i.id_equipement = e.id_equipement
            GROUP BY s.id_station, s.nom
            ORDER BY s.nom
            """, nativeQuery = true)
    List<IncidentsParStation> compterIncidentsParStation();

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

    @Query("""
            select i from Incident i
            join fetch i.equipement e
            join fetch e.station
            left join fetch i.prestataire
            where (:statut is null or i.statut = :statut)
            order by i.dateHeureOuverture desc
            """)
    List<Incident> trouverIncidents(@Param("statut") StatutIncident statut);
}