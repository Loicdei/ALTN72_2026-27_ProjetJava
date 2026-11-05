package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.IncidentsParStation;
import altn72.tpfilrouge.modele.NombreIncidentsParType;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.modele.UtilisateurDetails;
import altn72.tpfilrouge.repository.EquipementRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import altn72.tpfilrouge.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
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
class StatistiquesControleurTest {

    private static final String ROUTE = "/statistiques";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private Equipement portique;
    private Equipement ecran;

    @BeforeEach
    void creerDonnees() {
        Station nord = stationRepository.save(new Station("Station Nord"));
        stationRepository.save(new Station("Station Vide"));
        portique = equipementRepository.save(new Equipement("EQ-1", "Portique", "Portique", "EN_SERVICE", nord));
        ecran = equipementRepository.save(new Equipement("EQ-2", "Écran", "Écran", "EN_SERVICE", nord));
        incidentRepository.save(new Incident("Actif", LocalDateTime.now().minusHours(1), portique));
        incidentRepository.save(new Incident("Actif aussi", LocalDateTime.now().minusHours(2), portique));
        Incident resolu = new Incident("Résolu", LocalDateTime.now().minusHours(3), ecran);
        resolu.setStatut(StatutIncident.RESOLU);
        resolu.setDateHeureResolution(LocalDateTime.now());
        incidentRepository.save(resolu);
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

    @Test
    void unUtilisateurNonConnecteEstRenvoyeVersLaConnexion() throws Exception {
        mockMvc.perform(get(ROUTE)).andExpect(status().is3xxRedirection());
    }

    @Test
    void unAgentNaPasAccesAuxStatistiques() throws Exception {
        mockMvc.perform(get(ROUTE).with(user(agentTerrain()))).andExpect(status().isForbidden());
    }

    @Test
    void seulLeResponsableVoitLeLienSurLAccueil() throws Exception {
        mockMvc.perform(get("/accueil").with(user(responsable())))
                .andExpect(content().string(containsString("href=\"/statistiques\"")));
        mockMvc.perform(get("/accueil").with(user(agentTerrain())))
                .andExpect(content().string(not(containsString("href=\"/statistiques\""))));
    }

    @Test
    void laPageTransmetLesDonneesAuxGraphiques() throws Exception {
        mockMvc.perform(get(ROUTE).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("graphiqueStations")))
                .andExpect(content().string(containsString("\"Station Nord\"")))
                .andExpect(content().string(containsString("/js/statistiques.js")));
    }

    @Test
    void laRequeteNativeCompteActifsEtResolusEtGardeLesStationsSansIncident() {
        List<IncidentsParStation> resultat = incidentRepository.compterIncidentsParStation();

        assertThat(resultat)
                .extracting(IncidentsParStation::getStation, IncidentsParStation::getNombreActifs, IncidentsParStation::getNombreResolus)
                .contains(
                        tuple("Station Nord", 2L, 1L),
                        tuple("Station Vide", 0L, 0L));
    }

    @Test
    void laRequeteParTypeTrieDuPlusFrequentAuMoinsFrequent() {
        assertThat(incidentRepository.compterIncidentsParType()).containsExactly(
                new NombreIncidentsParType("Portique", 2L),
                new NombreIncidentsParType("Écran", 1L));
    }
}
