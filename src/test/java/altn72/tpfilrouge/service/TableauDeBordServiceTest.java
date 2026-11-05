package altn72.tpfilrouge.service;

import altn72.tpfilrouge.formulaire.FiltreTableauDeBord;
import altn72.tpfilrouge.modele.CompteurNiveauAttention;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.LigneTableauDeBord;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.modele.TableauDeBord;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableauDeBordServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final FiltreTableauDeBord SANS_FILTRE = new FiltreTableauDeBord(null, null, null);

    @Mock
    private IncidentRepository incidentRepository;

    private TableauDeBordService tableauDeBordService;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        Clock horloge = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        IncidentService incidentService = new IncidentService(incidentRepository,
                mock(EquipementService.class), mock(PrestataireService.class), horloge);
        tableauDeBordService = new TableauDeBordService(incidentService);
    }

    private Incident incidentNonAffecteOuvertIlYA(int heures) {
        return new Incident("Ouvert il y a " + heures + " h", MAINTENANT.minusHours(heures), new Equipement());
    }

    private Incident incidentAffecteOuvertIlYA(int heures) {
        Incident incident = incidentNonAffecteOuvertIlYA(heures);
        incident.setPrestataire(new Prestataire());
        incident.setStatut(StatutIncident.AFFECTE);
        return incident;
    }

    private void lesIncidentsActifsSont(Incident... incidents) {
        when(incidentRepository.trouverIncidentsActifs(null, null)).thenReturn(List.of(incidents));
    }

    private List<String> descriptions(TableauDeBord tableau) {
        return tableau.lignes().stream().map(ligne -> ligne.incident().getDescription()).toList();
    }

    private long nombreAuNiveau(TableauDeBord tableau, NiveauAttention niveau) {
        return tableau.compteurs().stream()
                .filter(compteur -> compteur.niveau() == niveau)
                .mapToLong(CompteurNiveauAttention::nombre)
                .sum();
    }

    @Test
    void chaqueLigneCalculeSonNiveauEtSaDureeAPartirDeLHorloge() {
        lesIncidentsActifsSont(incidentNonAffecteOuvertIlYA(47));

        LigneTableauDeBord ligne = tableauDeBordService.construireTableauDeBord(SANS_FILTRE).lignes().getFirst();

        assertThat(ligne.niveauAttention()).isEqualTo(NiveauAttention.VERT);
        assertThat(ligne.dureeOuverture()).isEqualTo(Duration.ofHours(47));
    }

    @Test
    void lesIncidentsSontTriesDuPlusUrgentAuMoinsUrgentPuisDuPlusAncienAuPlusRecent() {
        lesIncidentsActifsSont(
                incidentAffecteOuvertIlYA(100),
                incidentNonAffecteOuvertIlYA(10),
                incidentNonAffecteOuvertIlYA(50),
                incidentNonAffecteOuvertIlYA(80),
                incidentNonAffecteOuvertIlYA(75));

        assertThat(descriptions(tableauDeBordService.construireTableauDeBord(SANS_FILTRE))).containsExactly(
                "Ouvert il y a 80 h",
                "Ouvert il y a 75 h",
                "Ouvert il y a 50 h",
                "Ouvert il y a 10 h",
                "Ouvert il y a 100 h");
    }

    @Test
    void lesCompteursDonnentLaRepartitionDesIncidentsActifs() {
        lesIncidentsActifsSont(
                incidentNonAffecteOuvertIlYA(10),
                incidentNonAffecteOuvertIlYA(20),
                incidentNonAffecteOuvertIlYA(50),
                incidentNonAffecteOuvertIlYA(80),
                incidentAffecteOuvertIlYA(100));

        TableauDeBord tableau = tableauDeBordService.construireTableauDeBord(SANS_FILTRE);

        assertThat(tableau.nombreIncidentsActifs()).isEqualTo(5);
        assertThat(nombreAuNiveau(tableau, NiveauAttention.VERT)).isEqualTo(2);
        assertThat(nombreAuNiveau(tableau, NiveauAttention.ORANGE)).isEqualTo(1);
        assertThat(nombreAuNiveau(tableau, NiveauAttention.ROUGE)).isEqualTo(1);
        assertThat(nombreAuNiveau(tableau, NiveauAttention.PRIS_EN_CHARGE)).isEqualTo(1);
    }

    @Test
    void leFiltreParNiveauRestreintLesLignesMaisPasLesCompteurs() {
        lesIncidentsActifsSont(incidentNonAffecteOuvertIlYA(10), incidentNonAffecteOuvertIlYA(80));

        TableauDeBord tableau = tableauDeBordService.construireTableauDeBord(
                new FiltreTableauDeBord(null, null, NiveauAttention.ROUGE));

        assertThat(descriptions(tableau)).containsExactly("Ouvert il y a 80 h");
        assertThat(tableau.nombreIncidentsActifs()).isEqualTo(2);
        assertThat(nombreAuNiveau(tableau, NiveauAttention.VERT)).isEqualTo(1);
    }

    @Test
    void lesFiltresDeStationEtDeTypeSontTransmisALaRequete() {
        when(incidentRepository.trouverIncidentsActifs(4L, "Portique"))
                .thenReturn(List.of(incidentNonAffecteOuvertIlYA(10)));

        TableauDeBord tableau = tableauDeBordService.construireTableauDeBord(
                new FiltreTableauDeBord(4L, "Portique", null));

        assertThat(descriptions(tableau)).containsExactly("Ouvert il y a 10 h");
    }

    @Test
    void unTypeVideEquivautAAucunFiltre() {
        assertThat(new FiltreTableauDeBord(null, "  ", null).type()).isNull();
    }

    @Test
    void lAttenteMaximaleIgnoreLesIncidentsDejaPrisEnCharge() {
        lesIncidentsActifsSont(incidentAffecteOuvertIlYA(100), incidentNonAffecteOuvertIlYA(10), incidentNonAffecteOuvertIlYA(50));

        assertThat(tableauDeBordService.construireTableauDeBord(SANS_FILTRE).attenteMaximaleSansPrestataire())
                .isEqualTo(Duration.ofHours(50));
    }

    @Test
    void sansIncidentEnAttenteIlNYAPasDAttenteMaximale() {
        lesIncidentsActifsSont(incidentAffecteOuvertIlYA(100));

        assertThat(tableauDeBordService.construireTableauDeBord(SANS_FILTRE).attenteMaximaleSansPrestataire()).isNull();
    }
}
