package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Evaluation;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.repository.EvaluationRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EvaluationRepository evaluationRepository;
    private final EquipementService equipementService;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository,
                           EvaluationRepository evaluationRepository,
                           EquipementService equipementService,
                           Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.evaluationRepository = evaluationRepository;
        this.equipementService = equipementService;
        this.horloge = horloge;
    }

    public List<Incident> obtenirIncidentsParEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateHeureOuvertureDesc(equipementId);
    }

    public Incident obtenirParId(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("L'incident n° " + id + " n'existe pas."));
    }

    public Optional<Evaluation> obtenirEvaluation(Long incidentId) {
        return evaluationRepository.findByIncidentId(incidentId);
    }

    public Duration calculerDureeOuverture(Incident incident) {
        LocalDateTime finOuverture = incident.getDateHeureResolution() != null
                ? incident.getDateHeureResolution()
                : LocalDateTime.now(horloge);
        return Duration.between(incident.getDateHeureOuverture(), finOuverture);
    }

    public Incident declarerIncident(Long equipementId, String description) {
        Equipement equipement = equipementService.obtenirParId(equipementId);
        Incident incident = new Incident(description, LocalDateTime.now(horloge), equipement);
        return incidentRepository.save(incident);
    }

    public Incident obtenirIncidentModifiable(Long id) {
        Incident incident = obtenirParId(id);
        if (incident.isResolu()) {
            throw new OperationIncidentInterditeException("Un incident résolu ne peut plus être modifié.");
        }
        return incident;
    }

    // La vérification du statut et l'écriture doivent voir le même état : une résolution ne peut pas s'intercaler.
    @Transactional
    public Incident modifierDescription(Long id, String description) {
        Incident incident = obtenirIncidentModifiable(id);
        incident.setDescription(description);
        return incident;
    }
}
