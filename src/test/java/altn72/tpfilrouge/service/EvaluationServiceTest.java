package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.DonneeInvalideException;
import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Evaluation;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.repository.EvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final Long ID_INCIDENT = 7L;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private IncidentService incidentService;

    private EvaluationService evaluationService;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        Clock horloge = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        evaluationService = new EvaluationService(evaluationRepository, incidentService, horloge);
    }

    private Incident incidentResoluAvecPrestataire() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusDays(2), new Equipement());
        incident.setId(ID_INCIDENT);
        incident.setPrestataire(new Prestataire());
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(MAINTENANT.minusDays(1));
        return incident;
    }

    @Test
    void evaluerUnIncidentResoluEnregistreLaNoteLeCommentaireEtLaDate() {
        Incident incident = incidentResoluAvecPrestataire();
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incident);
        when(evaluationRepository.save(any(Evaluation.class))).then(returnsFirstArg());

        Evaluation evaluation = evaluationService.evaluerIntervention(ID_INCIDENT, 4, "  Intervention soignée ");

        assertThat(evaluation.getNote()).isEqualTo(4);
        assertThat(evaluation.getCommentaire()).isEqualTo("Intervention soignée");
        assertThat(evaluation.getDateEvaluation()).isEqualTo(MAINTENANT);
        assertThat(evaluation.getIncident()).isSameAs(incident);
    }

    @ParameterizedTest
    @ValueSource(ints = {Evaluation.NOTE_MINIMALE, Evaluation.NOTE_MAXIMALE})
    void lesNotesAuxBornesSontAcceptees(int note) {
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incidentResoluAvecPrestataire());
        when(evaluationRepository.save(any(Evaluation.class))).then(returnsFirstArg());

        assertThat(evaluationService.evaluerIntervention(ID_INCIDENT, note, null).getNote()).isEqualTo(note);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {Evaluation.NOTE_MINIMALE - 1, Evaluation.NOTE_MAXIMALE + 1})
    void uneNoteHorsDesBornesEstRefusee(Integer note) {
        assertThatThrownBy(() -> evaluationService.evaluerIntervention(ID_INCIDENT, note, null))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessage("La note doit être un entier de 1 à 5.");

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void unCommentaireVideNEstPasConserve() {
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incidentResoluAvecPrestataire());
        when(evaluationRepository.save(any(Evaluation.class))).then(returnsFirstArg());

        assertThat(evaluationService.evaluerIntervention(ID_INCIDENT, 4, "   ").getCommentaire()).isNull();
    }

    @Test
    void unIncidentNonResoluNePeutPasEtreEvalue() {
        Incident incident = incidentResoluAvecPrestataire();
        incident.setStatut(StatutIncident.AFFECTE);
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incident);

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(ID_INCIDENT, 4, null))
                .isInstanceOf(OperationIncidentInterditeException.class)
                .hasMessage("Seul un incident résolu peut être évalué.");

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void unIncidentResoluSansPrestataireNePeutPasEtreEvalue() {
        Incident incident = incidentResoluAvecPrestataire();
        incident.setPrestataire(null);
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incident);

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(ID_INCIDENT, 4, null))
                .isInstanceOf(OperationIncidentInterditeException.class);

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void unIncidentDejaEvalueNePeutPasLEtreUneSecondeFois() {
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incidentResoluAvecPrestataire());
        when(evaluationRepository.existsByIncidentId(ID_INCIDENT)).thenReturn(true);

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(ID_INCIDENT, 4, null))
                .isInstanceOf(OperationIncidentInterditeException.class)
                .hasMessage("Cet incident a déjà été évalué.");

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void deuxSaisiesSimultaneesSontDepartageesParLaContrainteDUnicite() {
        when(incidentService.obtenirParId(ID_INCIDENT)).thenReturn(incidentResoluAvecPrestataire());
        when(evaluationRepository.save(any(Evaluation.class))).thenThrow(new DataIntegrityViolationException("doublon"));

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(ID_INCIDENT, 4, null))
                .isInstanceOf(OperationIncidentInterditeException.class)
                .hasMessage("Cet incident a déjà été évalué.");
    }

    @Test
    void seulUnIncidentResoluAvecPrestataireEtSansEvaluationEstEvaluable() {
        assertThat(evaluationService.estEvaluable(incidentResoluAvecPrestataire())).isTrue();

        Incident nonResolu = incidentResoluAvecPrestataire();
        nonResolu.setStatut(StatutIncident.AFFECTE);
        assertThat(evaluationService.estEvaluable(nonResolu)).isFalse();

        Incident sansPrestataire = incidentResoluAvecPrestataire();
        sansPrestataire.setPrestataire(null);
        assertThat(evaluationService.estEvaluable(sansPrestataire)).isFalse();

        when(evaluationRepository.existsByIncidentId(ID_INCIDENT)).thenReturn(true);
        assertThat(evaluationService.estEvaluable(incidentResoluAvecPrestataire())).isFalse();
    }
}
