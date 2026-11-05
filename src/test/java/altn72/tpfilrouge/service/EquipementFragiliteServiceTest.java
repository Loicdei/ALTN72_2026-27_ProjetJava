package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.repository.EquipementRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import altn72.tpfilrouge.repository.PrestataireRepository;
import altn72.tpfilrouge.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// Passe par la vraie requête, avec une horloge fixe, pour vérifier les cas limites à la minute près.
@SpringBootTest
@Transactional
class EquipementFragiliteServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 15, 0);
    private static final LocalDateTime IL_Y_A_UN_JOUR = MAINTENANT.minusDays(1);
    private static final LocalDateTime IL_Y_A_DEUX_JOURS = MAINTENANT.minusDays(2);

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PrestataireRepository prestataireRepository;

    private EquipementFragiliteService fragiliteService;
    private Station station;
    private Equipement equipement;

    @BeforeEach
    void creerServiceAvecHorlogeFixe() {
        ZoneId fuseau = ZoneId.systemDefault();
        Clock horloge = Clock.fixed(MAINTENANT.atZone(fuseau).toInstant(), fuseau);
        fragiliteService = new EquipementFragiliteService(incidentRepository, horloge);
        station = stationRepository.save(new Station("Station Test"));
        equipement = creerEquipement("EQ-TEST-1");
    }

    private Equipement creerEquipement(String reference) {
        return equipementRepository.save(new Equipement(reference, "Portique", "Portique", "EN_SERVICE", station));
    }

    private Incident ouvrirIncident(Equipement concerne, LocalDateTime dateHeureOuverture) {
        return incidentRepository.save(new Incident("Panne", dateHeureOuverture, concerne));
    }

    private void ouvrirIncidents(Equipement concerne, LocalDateTime... datesOuverture) {
        for (LocalDateTime dateHeureOuverture : datesOuverture) {
            ouvrirIncident(concerne, dateHeureOuverture);
        }
    }

    @Test
    void deuxIncidentsDansLaFenetreNeRendentPasFragile() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS);

        assertThat(fragiliteService.estFragile(equipement.getId())).isFalse();
    }

    @Test
    void troisIncidentsDansLaFenetreRendentFragile() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT);

        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();
    }

    @Test
    void unIncidentResoluCompteAussi() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS);
        Incident resolu = ouvrirIncident(equipement, MAINTENANT.minusDays(3));
        resolu.setPrestataire(prestataireRepository.save(new Prestataire("Rihanna", "a@b.fr", "0100000000")));
        resolu.setStatut(StatutIncident.RESOLU);
        resolu.setDateHeureResolution(MAINTENANT);

        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();
    }

    @Test
    void unIncidentOuvertIlYATrenteJoursEtUneMinuteNeComptePlus() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT.minusDays(30).minusMinutes(1));

        assertThat(fragiliteService.estFragile(equipement.getId())).isFalse();
    }

    @Test
    void unIncidentOuvertIlYAVingtNeufJoursEtVingtTroisHeuresCinquanteNeufCompte() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT.minusDays(29).minusHours(23).minusMinutes(59));

        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();
    }

    @Test
    void unIncidentOuvertIlYAExactementTrenteJoursCompte() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT.minusDays(30));

        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();
    }

    @Test
    void deuxIncidentsRecentsEtCinqVieuxNeRendentPasFragile() {
        LocalDateTime ilYA45Jours = MAINTENANT.minusDays(45);
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS,
                ilYA45Jours, ilYA45Jours, ilYA45Jours, ilYA45Jours, ilYA45Jours);

        assertThat(fragiliteService.estFragile(equipement.getId())).isFalse();
    }

    @Test
    void lEquipementVoisinNestPasContamine() {
        Equipement voisin = creerEquipement("EQ-TEST-2");
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT);
        ouvrirIncidents(voisin, IL_Y_A_UN_JOUR);

        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();
        assertThat(fragiliteService.estFragile(voisin.getId())).isFalse();
        assertThat(fragiliteService.obtenirEquipementsFragiles()).containsExactly(equipement.getId());
    }

    @Test
    void laFenetreGlisseAvecLHeureDeConsultation() {
        ouvrirIncidents(equipement, IL_Y_A_UN_JOUR, IL_Y_A_DEUX_JOURS, MAINTENANT.minusDays(29));
        assertThat(fragiliteService.estFragile(equipement.getId())).isTrue();

        ZoneId fuseau = ZoneId.systemDefault();
        Clock deuxJoursPlusTard = Clock.fixed(MAINTENANT.plusDays(2).atZone(fuseau).toInstant(), fuseau);
        EquipementFragiliteService plusTard = new EquipementFragiliteService(incidentRepository, deuxJoursPlusTard);

        assertThat(plusTard.estFragile(equipement.getId())).isFalse();
    }
}
