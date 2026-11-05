package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.NombreIncidentsParType;
import altn72.tpfilrouge.modele.NotePrestataire;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.SerieStatistique;
import altn72.tpfilrouge.modele.StatistiquesIncidents;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatistiquesServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 10, 0);

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private PrestataireService prestataireService;

    private StatistiquesService statistiquesService;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        Clock horloge = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        IncidentService incidentService = new IncidentService(incidentRepository,
                mock(EquipementService.class), prestataireService, horloge);
        statistiquesService = new StatistiquesService(incidentRepository, incidentService, prestataireService, horloge);
    }

    private Prestataire prestataire(String nom) {
        return new Prestataire(nom, "a@b.fr", "0100000000");
    }

    private Incident incidentOuvertLe(LocalDateTime dateHeureOuverture) {
        return new Incident("Panne", dateHeureOuverture, new Equipement());
    }

    private Incident incidentResoluEn(int heures, Prestataire prestataire) {
        Incident incident = incidentOuvertLe(MAINTENANT.minusDays(1));
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(incident.getDateHeureOuverture().plusHours(heures));
        return incident;
    }

    @Test
    void laTendanceCouvreLes14DerniersJoursEtMontreLesJoursSansIncident() {
        when(incidentRepository.findByDateHeureOuvertureGreaterThanEqual(LocalDateTime.of(2026, 10, 23, 0, 0)))
                .thenReturn(List.of(
                        incidentOuvertLe(LocalDateTime.of(2026, 10, 23, 0, 0)),
                        incidentOuvertLe(MAINTENANT.minusHours(1)),
                        incidentOuvertLe(MAINTENANT.minusHours(2))));

        SerieStatistique tendance = statistiquesService.calculerStatistiques().ouverturesParJour();

        assertThat(tendance.libelles()).hasSize(14).startsWith("23/10").endsWith("05/11");
        assertThat(tendance.valeurs()).hasSize(14);
        assertThat(tendance.valeurs().getFirst().longValue()).isEqualTo(1);
        assertThat(tendance.valeurs().get(1).longValue()).isZero();
        assertThat(tendance.valeurs().getLast().longValue()).isEqualTo(2);
    }

    @Test
    void lesNotesNeGardentQueLesPrestatairesEvaluesEtSontArrondies() {
        when(prestataireService.obtenirNotesDesPrestataires()).thenReturn(List.of(
                new NotePrestataire(prestataire("Rihanna"), 4.25, 4L),
                new NotePrestataire(prestataire("Zaho"), null, 0L)));

        SerieStatistique notes = statistiquesService.calculerStatistiques().notesParPrestataire();

        assertThat(notes.libelles()).containsExactly("Rihanna");
        assertThat(notes.valeurs()).extracting(valeur -> (BigDecimal) valeur).containsExactly(new BigDecimal("4.3"));
    }

    @Test
    void leDelaiMoyenDeResolutionEstCalculeEnHeuresParPrestataire() {
        Prestataire rihanna = prestataire("Rihanna");
        Prestataire aya = prestataire("Aya");
        when(incidentRepository.findByStatutOrderByDateHeureResolutionDesc(StatutIncident.RESOLU)).thenReturn(List.of(
                incidentResoluEn(2, rihanna),
                incidentResoluEn(5, rihanna),
                incidentResoluEn(10, aya)));

        SerieStatistique delais = statistiquesService.calculerStatistiques().delaisResolutionParPrestataire();

        assertThat(delais.libelles()).containsExactly("Aya", "Rihanna");
        assertThat(delais.valeurs()).extracting(valeur -> (BigDecimal) valeur)
                .containsExactly(new BigDecimal("10.0"), new BigDecimal("3.5"));
    }

    @Test
    void lesIncidentsParTypeReprennentLeResultatDeLaRequete() {
        when(incidentRepository.compterIncidentsParType()).thenReturn(List.of(
                new NombreIncidentsParType("Portique", 3L),
                new NombreIncidentsParType("Écran", 1L)));

        SerieStatistique types = statistiquesService.calculerStatistiques().incidentsParType();

        assertThat(types.libelles()).containsExactly("Portique", "Écran");
        assertThat(types.valeurs()).extracting(Number::longValue).containsExactly(3L, 1L);
    }

    @Test
    void sansDonneesToutesLesSeriesSontVidesSaufLaTendance() {
        StatistiquesIncidents statistiques = statistiquesService.calculerStatistiques();

        assertThat(statistiques.stations()).isEmpty();
        assertThat(statistiques.notesParPrestataire().libelles()).isEmpty();
        assertThat(statistiques.ouverturesParJour().valeurs()).hasSize(14)
                .allSatisfy(valeur -> assertThat(valeur.longValue()).isZero());
    }
}
