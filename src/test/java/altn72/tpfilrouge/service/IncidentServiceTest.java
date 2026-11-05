package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EquipementService equipementService;

    private IncidentService incidentService;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        Clock horloge = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        incidentService = new IncidentService(incidentRepository, equipementService, horloge);
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
