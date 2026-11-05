package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.DonneeInvalideException;
import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.modele.Evaluation;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.repository.EvaluationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class EvaluationService {

    private static final String MESSAGE_DEJA_EVALUE = "Cet incident a déjà été évalué.";

    private final EvaluationRepository evaluationRepository;
    private final IncidentService incidentService;
    private final Clock horloge;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             IncidentService incidentService,
                             Clock horloge) {
        this.evaluationRepository = evaluationRepository;
        this.incidentService = incidentService;
        this.horloge = horloge;
    }

    public Optional<Evaluation> obtenirEvaluation(Long incidentId) {
        return evaluationRepository.findByIncidentId(incidentId);
    }

    public Map<Long, Integer> obtenirNotesParIncident(List<Incident> incidents) {
        return evaluationRepository.findByIncidentIn(incidents).stream()
                .collect(Collectors.toMap(evaluation -> evaluation.getIncident().getId(), Evaluation::getNote));
    }

    public List<Integer> obtenirNotesPossibles() {
        return IntStream.rangeClosed(Evaluation.NOTE_MINIMALE, Evaluation.NOTE_MAXIMALE).boxed().toList();
    }

    public boolean estEvaluable(Incident incident) {
        return incident.isResolu()
                && incident.getPrestataire() != null
                && !evaluationRepository.existsByIncidentId(incident.getId());
    }

    public Evaluation evaluerIntervention(Long incidentId, Integer note, String commentaire) {
        verifierNote(note);
        Incident incident = incidentService.obtenirParId(incidentId);
        verifierEvaluationPossible(incident);
        Evaluation evaluation = new Evaluation(note, normaliserCommentaire(commentaire), LocalDateTime.now(horloge), incident);
        try {
            return evaluationRepository.save(evaluation);
        } catch (DataIntegrityViolationException exception) {
            // Deux saisies simultanées peuvent passer la vérification : la contrainte d'unicité de la base les départage (RG8).
            throw new OperationIncidentInterditeException(MESSAGE_DEJA_EVALUE, exception);
        }
    }

    private void verifierNote(Integer note) {
        if (note == null || note < Evaluation.NOTE_MINIMALE || note > Evaluation.NOTE_MAXIMALE) {
            throw new DonneeInvalideException(
                    "La note doit être un entier de " + Evaluation.NOTE_MINIMALE + " à " + Evaluation.NOTE_MAXIMALE + ".");
        }
    }

    private void verifierEvaluationPossible(Incident incident) {
        if (!incident.isResolu()) {
            throw new OperationIncidentInterditeException("Seul un incident résolu peut être évalué.");
        }
        if (incident.getPrestataire() == null) {
            throw new OperationIncidentInterditeException("Cet incident n'a été affecté à aucun prestataire : il ne peut pas être évalué.");
        }
        if (evaluationRepository.existsByIncidentId(incident.getId())) {
            throw new OperationIncidentInterditeException(MESSAGE_DEJA_EVALUE);
        }
    }

    private String normaliserCommentaire(String commentaire) {
        return StringUtils.hasText(commentaire) ? commentaire.strip() : null;
    }
}
