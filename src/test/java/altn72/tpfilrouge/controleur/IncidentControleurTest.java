package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentControleurTest {

    private static final Long ID_INEXISTANT = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private Equipement equipement;

    @BeforeEach
    void creerEquipement() {
        Station station = stationRepository.save(new Station("Station Test"));
        equipement = equipementRepository.save(
                new Equipement("EQ-TEST-001", "Portique de test", "Portique", "EN_SERVICE", station));
    }

    private List<Incident> incidentsDeLEquipement() {
        return incidentRepository.findByEquipementIdOrderByDateHeureOuvertureDesc(equipement.getId());
    }

    @Test
    void unUtilisateurNonConnecteEstRenvoyeVersLaConnexion() throws Exception {
        mockMvc.perform(get("/equipements/{id}/incidents/nouveau", equipement.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void leFormulaireDeDeclarationRappelleLEquipementConcerne() throws Exception {
        mockMvc.perform(get("/equipements/{id}/incidents/nouveau", equipement.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EQ-TEST-001")));
    }

    @Test
    @WithMockUser
    void uneDeclarationValideCreeLIncidentPuisRevientSurLaFiche() throws Exception {
        mockMvc.perform(post("/equipements/{id}/incidents", equipement.getId())
                        .param("description", "Portique bloqué"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipements/" + equipement.getId()));

        List<Incident> incidents = incidentsDeLEquipement();
        assertThat(incidents).hasSize(1);
        assertThat(incidents.getFirst().getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    @WithMockUser
    void uneDescriptionVideEstRefuseeAvecUnMessageComprehensible() throws Exception {
        mockMvc.perform(post("/equipements/{id}/incidents", equipement.getId())
                        .param("description", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La description est obligatoire.")));

        assertThat(incidentsDeLEquipement()).isEmpty();
    }

    @Test
    void unEquipementInexistantDonneUne404LisibleQuiConserveLUtilisateurConnecte() throws Exception {
        UtilisateurDetails agent = new UtilisateurDetails("agent", "secret",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT_TERRAIN")), "Camille", "AGENT_TERRAIN");

        mockMvc.perform(get("/equipements/{id}/incidents/nouveau", ID_INEXISTANT).with(user(agent)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("n&#39;existe pas")))
                .andExpect(content().string(containsString("Camille")));
    }
}
