package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.LigneTableauDeBord;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.modele.TableauDeBord;
import altn72.tpfilrouge.modele.UtilisateurDetails;
import altn72.tpfilrouge.repository.EquipementRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import altn72.tpfilrouge.repository.PrestataireRepository;
import altn72.tpfilrouge.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TableauDeBordControleurTest {

    private static final String ROUTE = "/tableau-de-bord";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PrestataireRepository prestataireRepository;

    private Station stationNord;
    private Station stationSud;
    private Equipement portiqueNord;
    private Equipement ecranSud;
    private Prestataire prestataire;

    @BeforeEach
    void creerDonnees() {
        stationNord = stationRepository.save(new Station("Station Nord"));
        stationSud = stationRepository.save(new Station("Station Sud"));
        portiqueNord = equipementRepository.save(new Equipement("EQ-N-1", "Portique nord", "Portique", "EN_SERVICE", stationNord));
        ecranSud = equipementRepository.save(new Equipement("EQ-S-1", "Écran sud", "Écran", "EN_SERVICE", stationSud));
        prestataire = prestataireRepository.save(new Prestataire("Rihanna", "a@b.fr", "0100000000"));
    }

    private UtilisateurDetails utilisateur(String profil) {
        return new UtilisateurDetails("login", "secret", List.of(new SimpleGrantedAuthority("ROLE_" + profil)), "Stromae", profil);
    }

    private UtilisateurDetails responsable() {
        return utilisateur("RESPONSABLE_EXPLOITATION");
    }

    private UtilisateurDetails agentTerrain() {
        return utilisateur("AGENT_TERRAIN");
    }

    private Incident creerIncidentNonAffecte(String description, int heures, Equipement equipement) {
        return incidentRepository.save(new Incident(description, LocalDateTime.now().minusHours(heures), equipement));
    }

    private Incident creerIncidentAffecte(String description, int heures, Equipement equipement) {
        Incident incident = new Incident(description, LocalDateTime.now().minusHours(heures), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.AFFECTE);
        return incidentRepository.save(incident);
    }

    private void creerIncidentResolu(String description, Equipement equipement) {
        Incident incident = new Incident(description, LocalDateTime.now().minusHours(100), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        incidentRepository.save(incident);
    }

    private TableauDeBord tableauAffiche(MockHttpServletRequestBuilder requete) throws Exception {
        return (TableauDeBord) mockMvc.perform(requete.with(user(responsable())))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("tableau");
    }

    private List<String> descriptions(TableauDeBord tableau) {
        return tableau.lignes().stream().map(ligne -> ligne.incident().getDescription()).toList();
    }

    @Test
    void unUtilisateurNonConnecteEstRenvoyeVersLaConnexion() throws Exception {
        mockMvc.perform(get(ROUTE)).andExpect(status().is3xxRedirection());
    }

    @Test
    void unAgentNaPasAccesAuTableauDeBordMemeEnAppelantLUrlDirectement() throws Exception {
        mockMvc.perform(get(ROUTE).with(user(agentTerrain()))).andExpect(status().isForbidden());
    }

    @Test
    void seulLeResponsableVoitLeLienVersLeTableauDeBordSurLAccueil() throws Exception {
        mockMvc.perform(get("/accueil").with(user(responsable())))
                .andExpect(content().string(containsString("href=\"/tableau-de-bord\"")));
        mockMvc.perform(get("/accueil").with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/tableau-de-bord\""))));
    }

    @Test
    void lesCasLimitesDesSeuilsSontRespectes() throws Exception {
        creerIncidentNonAffecte("47 heures", 47, portiqueNord);
        creerIncidentNonAffecte("48 heures", 48, portiqueNord);
        creerIncidentNonAffecte("72 heures", 72, portiqueNord);

        assertThat(tableauAffiche(get(ROUTE)).lignes())
                .extracting(ligne -> ligne.incident().getDescription(), LigneTableauDeBord::niveauAttention)
                .containsExactly(
                        tuple("72 heures", NiveauAttention.ROUGE),
                        tuple("48 heures", NiveauAttention.ORANGE),
                        tuple("47 heures", NiveauAttention.VERT));
    }

    @Test
    void unIncidentAffecteNePorteJamaisLIndicateurDUrgence() throws Exception {
        creerIncidentAffecte("Affecté depuis longtemps", 200, portiqueNord);

        assertThat(tableauAffiche(get(ROUTE)).lignes().getFirst().niveauAttention())
                .isEqualTo(NiveauAttention.PRIS_EN_CHARGE);
    }

    @Test
    void unIncidentResoluNApparaitPasParmiLesIncidentsActifs() throws Exception {
        creerIncidentResolu("Déjà résolu", portiqueNord);
        creerIncidentNonAffecte("Encore ouvert", 1, portiqueNord);

        assertThat(descriptions(tableauAffiche(get(ROUTE)))).containsExactly("Encore ouvert");
    }

    @Test
    void lesFiltresParStationTypeEtNiveauSeCombinent() throws Exception {
        creerIncidentNonAffecte("Nord récent", 1, portiqueNord);
        creerIncidentNonAffecte("Nord ancien", 80, portiqueNord);
        creerIncidentNonAffecte("Sud ancien", 80, ecranSud);

        assertThat(descriptions(tableauAffiche(get(ROUTE).param("stationId", stationSud.getId().toString()))))
                .containsExactly("Sud ancien");
        assertThat(descriptions(tableauAffiche(get(ROUTE).param("type", "Portique"))))
                .containsExactlyInAnyOrder("Nord récent", "Nord ancien");
        assertThat(descriptions(tableauAffiche(get(ROUTE).param("niveau", "ROUGE"))))
                .containsExactlyInAnyOrder("Nord ancien", "Sud ancien");
        assertThat(descriptions(tableauAffiche(get(ROUTE)
                .param("stationId", stationNord.getId().toString())
                .param("type", "Portique")
                .param("niveau", "ROUGE"))))
                .containsExactly("Nord ancien");
    }

    @Test
    void desFiltresVidesAffichentTousLesIncidentsActifs() throws Exception {
        creerIncidentNonAffecte("Nord", 1, portiqueNord);
        creerIncidentNonAffecte("Sud", 1, ecranSud);

        assertThat(descriptions(tableauAffiche(get(ROUTE)
                .param("stationId", "")
                .param("type", "")
                .param("niveau", ""))))
                .containsExactlyInAnyOrder("Nord", "Sud");
    }

    @Test
    void unNiveauInconnuEstRefuseSansTraceTechnique() throws Exception {
        mockMvc.perform(get(ROUTE).param("niveau", "VIOLET").with(user(responsable())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void chaqueLigneSitueLIncidentEtMeneASonDetail() throws Exception {
        Incident incident = creerIncidentNonAffecte("Portique bloqué", 1, portiqueNord);

        mockMvc.perform(get(ROUTE).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/incidents/" + incident.getId() + "\"")))
                .andExpect(content().string(containsString("Station Nord")))
                .andExpect(content().string(containsString("EQ-N-1 - Portique nord")))
                .andExpect(content().string(containsString("En attente d&#39;affectation")));
    }

    @Test
    void sansIncidentActifLaPageLeSignale() throws Exception {
        mockMvc.perform(get(ROUTE).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aucun incident actif")));
    }
}
