package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByEquipementIdOrderByDateHeureOuvertureDesc(Long equipementId);

    List<Incident> findByStatutOrderByDateHeureResolutionDesc(StatutIncident statut);
}