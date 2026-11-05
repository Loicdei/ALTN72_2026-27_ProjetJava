package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EquipementService equipementService;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository, EquipementService equipementService, Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementService = equipementService;
        this.horloge = horloge;
    }

    public List<Incident> obtenirIncidentsParEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateHeureOuvertureDesc(equipementId);
    }

    public Incident declarerIncident(Long equipementId, String description) {
        Equipement equipement = equipementService.obtenirParId(equipementId);
        Incident incident = new Incident(description, LocalDateTime.now(horloge), equipement);
        return incidentRepository.save(incident);
    }
}
