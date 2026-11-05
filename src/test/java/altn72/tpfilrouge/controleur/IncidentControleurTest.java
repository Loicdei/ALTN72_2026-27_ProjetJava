package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Evaluation;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.modele.UtilisateurDetails;
import altn72.tpfilrouge.repository.EquipementRepository;
import altn72.tpfilrouge.repository.EvaluationRepository;
import altn72.tpfilrouge.repository.IncidentRepository;
import altn72.tpfilrouge.repository.PrestataireRepository;
import altn72.tpfilrouge.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

    @Autowired
    private PrestataireRepository prestataireRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

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
    void uneDeclarationValideCreeLIncidentPuisAfficheSonDetail() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/equipements/{id}/incidents", equipement.getId())
                        .param("description", "Portique bloqué"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        List<Incident> incidents = incidentsDeLEquipement();
        assertThat(incidents).hasSize(1);
        Incident incident = incidents.getFirst();
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
        assertThat(resultat.getResponse().getRedirectedUrl()).isEqualTo("/incidents/" + incident.getId());
    }

    @Test
    @WithMockUser
    void leDetailDUnIncidentNonAffecteAfficheSesInformationsEtSaDuree() throws Exception {
        Incident incident = incidentRepository.save(
                new Incident("Portique bloqué", LocalDateTime.now().minusHours(50), equipement));

        mockMvc.perform(get("/incidents/{id}", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Portique bloqué")))
                .andExpect(content().string(containsString("EQ-TEST-001")))
                .andExpect(content().string(containsString("Station Test")))
                .andExpect(content().string(containsString("2 j 2 h")))
                .andExpect(content().string(containsString("Aucun prestataire affecté")))
                .andExpect(content().string(not(containsString("Évaluation de l&#39;intervention"))));
    }

    @Test
    @WithMockUser
    void leDetailDUnIncidentEvalueAffichePrestataireEtEvaluation() throws Exception {
        Prestataire prestataire = prestataireRepository.save(new Prestataire("Maintenance Test", "a@b.fr", "0100000000"));
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(50), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        incidentRepository.save(incident);
        evaluationRepository.save(new Evaluation(4, "Intervention soignée", LocalDateTime.now(), incident));

        mockMvc.perform(get("/incidents/{id}", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maintenance Test")))
                .andExpect(content().string(containsString("4 / 5")))
                .andExpect(content().string(containsString("Intervention soignée")));
    }

    @Test
    @WithMockUser
    void unIncidentInexistantDonneUne404() throws Exception {
        mockMvc.perform(get("/incidents/{id}", ID_INEXISTANT))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("n&#39;existe pas")));
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
