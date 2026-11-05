package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.repository.EvaluationRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final Long ID_EQUIPEMENT = 1L;
    private static final Long ID_INCIDENT = 7L;
    private static final Long ID_PRESTATAIRE = 3L;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private EquipementService equipementService;

    @Mock
    private PrestataireService prestataireService;

    private IncidentService incidentService;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        Clock horloge = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        incidentService = new IncidentService(incidentRepository, evaluationRepository, equipementService,
                prestataireService, horloge);
    }

    @Test
    void affecterUnPrestataireFaitPasserLIncidentAuStatutAffecte() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(1), new Equipement());
        Prestataire prestataire = new Prestataire();
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));
        when(prestataireService.obtenirParId(ID_PRESTATAIRE)).thenReturn(prestataire);

        incidentService.affecterPrestataire(ID_INCIDENT, ID_PRESTATAIRE);

        assertThat(incident.getPrestataire()).isSameAs(prestataire);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);
    }

    @Test
    void unIncidentAffectePeutEtreReaffecteAUnAutrePrestataire() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(1), new Equipement());
        incident.setPrestataire(new Prestataire());
        incident.setStatut(StatutIncident.AFFECTE);
        Prestataire nouveauPrestataire = new Prestataire();
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));
        when(prestataireService.obtenirParId(ID_PRESTATAIRE)).thenReturn(nouveauPrestataire);

        incidentService.affecterPrestataire(ID_INCIDENT, ID_PRESTATAIRE);

        assertThat(incident.getPrestataire()).isSameAs(nouveauPrestataire);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);
    }

    @Test
    void unIncidentResoluNePeutPlusEtreAffecte() {
        Prestataire prestataireInitial = new Prestataire();
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(2), new Equipement());
        incident.setPrestataire(prestataireInitial);
        incident.setStatut(StatutIncident.RESOLU);
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.affecterPrestataire(ID_INCIDENT, ID_PRESTATAIRE))
                .isInstanceOf(OperationIncidentInterditeException.class)
                .hasMessage("Un incident résolu ne peut plus être affecté ni réaffecté.");
        assertThat(incident.getPrestataire()).isSameAs(prestataireInitial);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.RESOLU);
    }

    @Test
    void affecterUnPrestataireInexistantLaisseLIncidentInchange() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(1), new Equipement());
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));
        when(prestataireService.obtenirParId(ID_PRESTATAIRE))
                .thenThrow(new RessourceIntrouvableException("Le prestataire n'existe pas."));

        assertThatThrownBy(() -> incidentService.affecterPrestataire(ID_INCIDENT, ID_PRESTATAIRE))
                .isInstanceOf(RessourceIntrouvableException.class);
        assertThat(incident.getPrestataire()).isNull();
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    void laDureeOuvertureDUnIncidentNonResoluVaJusquAMaintenant() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(47), new Equipement());

        assertThat(incidentService.calculerDureeOuverture(incident)).isEqualTo(Duration.ofHours(47));
    }

    @Test
    void laDureeOuvertureDUnIncidentResoluSArreteASaResolution() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(100), new Equipement());
        incident.setDateHeureResolution(MAINTENANT.minusHours(90));

        assertThat(incidentService.calculerDureeOuverture(incident)).isEqualTo(Duration.ofHours(10));
    }

    @Test
    void demanderUnIncidentInexistantEstRefuseAvecUnMessageEnFrancais() {
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.obtenirParId(ID_INCIDENT))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessage("L'incident n° 7 n'existe pas.");
    }

    @Test
    void laDescriptionDUnIncidentNonResoluPeutEtreModifiee() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(1), new Equipement());
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));

        Incident modifie = incidentService.modifierDescription(ID_INCIDENT, "Écran figé sur le quai 2");

        assertThat(modifie.getDescription()).isEqualTo("Écran figé sur le quai 2");
    }

    @Test
    void laDescriptionDUnIncidentResoluNePeutPlusEtreModifiee() {
        Incident incident = new Incident("Écran figé", MAINTENANT.minusHours(1), new Equipement());
        incident.setStatut(StatutIncident.RESOLU);
        when(incidentRepository.findById(ID_INCIDENT)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.modifierDescription(ID_INCIDENT, "Autre description"))
                .isInstanceOf(OperationIncidentInterditeException.class)
                .hasMessage("Un incident résolu ne peut plus être modifié.");
        assertThat(incident.getDescription()).isEqualTo("Écran figé");
    }

    @Test
    void unIncidentDeclareEstRattacheALEquipementEtOuvertMaintenantSansPrestataire() {
        Equipement equipement = new Equipement();
        when(equipementService.obtenirParId(ID_EQUIPEMENT)).thenReturn(equipement);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(appel -> appel.getArgument(0));

        Incident incident = incidentService.declarerIncident(ID_EQUIPEMENT, "Écran figé");

        assertThat(incident.getEquipement()).isSameAs(equipement);
        assertThat(incident.getDescription()).isEqualTo("Écran figé");
        assertThat(incident.getDateHeureOuverture()).isEqualTo(MAINTENANT);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
        assertThat(incident.getPrestataire()).isNull();
        assertThat(incident.getDateHeureResolution()).isNull();
    }

    @Test
    void declarerUnIncidentSurUnEquipementInexistantEstRefuse() {
        when(equipementService.obtenirParId(ID_EQUIPEMENT))
                .thenThrow(new RessourceIntrouvableException("L'équipement n'existe pas."));

        assertThatThrownBy(() -> incidentService.declarerIncident(ID_EQUIPEMENT, "Écran figé"))
                .isInstanceOf(RessourceIntrouvableException.class);
        verify(incidentRepository, never()).save(any());
    }
}
