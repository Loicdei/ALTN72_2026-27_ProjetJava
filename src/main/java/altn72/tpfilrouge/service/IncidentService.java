package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.modele.StatutIncident;
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
    private final EquipementService equipementService;
    private final PrestataireService prestataireService;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository,
                           EquipementService equipementService,
                           PrestataireService prestataireService,
                           Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementService = equipementService;
        this.prestataireService = prestataireService;
        this.horloge = horloge;
    }

    public List<Incident> obtenirIncidentsParEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateHeureOuvertureDesc(equipementId);
    }

    public List<Incident> obtenirHistoriqueDesIncidentsResolus() {
        return incidentRepository.findByStatutOrderByDateHeureResolutionDesc(StatutIncident.RESOLU);
    }

    public List<Incident> obtenirIncidentsActifs(Long stationId, String type) {
        return incidentRepository.trouverIncidentsActifs(stationId, type);
    }

    public List<Incident> obtenirIncidents(StatutIncident statut) {
        return incidentRepository.trouverIncidents(statut);
    }

    public Incident obtenirParId(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("L'incident n° " + id + " n'existe pas."));
    }

    public Duration calculerDureeOuverture(Incident incident) {
        LocalDateTime finOuverture = incident.getDateHeureResolution() != null
                ? incident.getDateHeureResolution()
                : LocalDateTime.now(horloge);
        return Duration.between(incident.getDateHeureOuverture(), finOuverture);
    }

    public Optional<NiveauAttention> calculerNiveauAttention(Incident incident) {
        if (incident.isResolu()) {
            return Optional.empty();
        }
        return Optional.of(NiveauAttention.determinerPourIncidentActif(incident.getStatut(), calculerDureeOuverture(incident)));
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

    // Le prestataire et le statut changent ensemble : un échec ne doit jamais laisser l'un sans l'autre (RG10).
    @Transactional
    public Incident affecterPrestataire(Long id, Long prestataireId) {
        Incident incident = obtenirParId(id);
        if (incident.isResolu()) {
            throw new OperationIncidentInterditeException("Un incident résolu ne peut plus être affecté ni réaffecté.");
        }
        incident.setPrestataire(prestataireService.obtenirParId(prestataireId));
        incident.setStatut(StatutIncident.AFFECTE);
        return incident;
    }

    // Le statut et la date de résolution changent ensemble : un incident résolu sans date serait incohérent (RG5, RG10).
    @Transactional
    public Incident declarerResolu(Long id) {
        Incident incident = obtenirParId(id);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        verifierResolutionPossible(incident, maintenant);
        incident.setDateHeureResolution(maintenant);
        incident.setStatut(StatutIncident.RESOLU);
        return incident;
    }

    private void verifierResolutionPossible(Incident incident, LocalDateTime dateHeureResolution) {
        if (incident.isResolu()) {
            throw new OperationIncidentInterditeException("Cet incident est déjà résolu.");
        }
        if (incident.getPrestataire() == null) {
            throw new OperationIncidentInterditeException("Affectez d'abord un prestataire pour pouvoir résoudre cet incident.");
        }
        if (dateHeureResolution.isBefore(incident.getDateHeureOuverture())) {
            throw new OperationIncidentInterditeException("La date de résolution ne peut pas précéder la date d'ouverture de l'incident.");
        }
    }
}
