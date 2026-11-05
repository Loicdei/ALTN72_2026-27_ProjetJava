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
        Prestataire prestataire = prestataireRepository.save(new Prestataire("Rihanna", "a@b.fr", "0100000000"));
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(50), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        incidentRepository.save(incident);
        evaluationRepository.save(new Evaluation(4, "Intervention soignée", LocalDateTime.now(), incident));

        mockMvc.perform(get("/incidents/{id}", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rihanna")))
                .andExpect(content().string(containsString("4 / 5")))
                .andExpect(content().string(containsString("Intervention soignée")));
    }

    private Incident creerIncidentOuvert() {
        return incidentRepository.save(new Incident("Portique bloqué", LocalDateTime.now().minusHours(1), equipement));
    }

    private Incident creerIncidentResolu() {
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(2), equipement);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        return incidentRepository.save(incident);
    }

    @Test
    @WithMockUser
    void leFormulaireDeModificationEstPreRempliEtAccessibleDepuisLeDetail() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(get("/incidents/{id}", incident.getId()))
                .andExpect(content().string(containsString("/incidents/" + incident.getId() + "/modifier")));
        mockMvc.perform(get("/incidents/{id}/modifier", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Portique bloqu&eacute;")));
    }

    @Test
    @WithMockUser
    void uneModificationValideEstEnregistreePuisAfficheLeDetail() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(post("/incidents/{id}/modifier", incident.getId())
                        .param("description", "Portique bloqué en position fermée"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/" + incident.getId()));

        assertThat(incident.getDescription()).isEqualTo("Portique bloqué en position fermée");
    }

    @Test
    @WithMockUser
    void uneModificationAvecDescriptionVideEstRefusee() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(post("/incidents/{id}/modifier", incident.getId()).param("description", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La description est obligatoire.")));

        assertThat(incident.getDescription()).isEqualTo("Portique bloqué");
    }

    @Test
    @WithMockUser
    void unIncidentResoluNeProposePasLaModificationEtLaRefuseCoteServeur() throws Exception {
        Incident incident = creerIncidentResolu();

        mockMvc.perform(get("/incidents/{id}", incident.getId()))
                .andExpect(content().string(not(containsString("/modifier"))));
        mockMvc.perform(get("/incidents/{id}/modifier", incident.getId()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Un incident résolu ne peut plus être modifié.")));
        mockMvc.perform(post("/incidents/{id}/modifier", incident.getId()).param("description", "Autre"))
                .andExpect(status().isConflict());

        assertThat(incident.getDescription()).isEqualTo("Portique bloqué");
    }

    private static UtilisateurDetails utilisateur(String prenom, String profil) {
        return new UtilisateurDetails(prenom, "secret", List.of(new SimpleGrantedAuthority("ROLE_" + profil)), prenom, profil);
    }

    private static UtilisateurDetails agentTerrain() {
        return utilisateur("Stromae", "AGENT_TERRAIN");
    }

    private static UtilisateurDetails responsable() {
        return utilisateur("Adele", UtilisateurDetails.PROFIL_RESPONSABLE_EXPLOITATION);
    }

    private Prestataire creerPrestataire(String nom) {
        return prestataireRepository.save(new Prestataire(nom, "contact@test.fr", "0100000000"));
    }

    private Incident creerIncidentAffecte() {
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(3), equipement);
        incident.setPrestataire(creerPrestataire("Beyonce"));
        incident.setStatut(StatutIncident.AFFECTE);
        return incidentRepository.save(incident);
    }

    @Test
    void leResponsablePeutDeclarerResoluUnIncidentAffecte() throws Exception {
        Incident incident = creerIncidentAffecte();

        mockMvc.perform(post("/incidents/{id}/resolution", incident.getId()).with(user(responsable())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/" + incident.getId()));

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.RESOLU);
        assertThat(incident.getDateHeureResolution()).isNotNull();
    }

    @Test
    void unAgentNePeutPasDeclarerResoluMemeEnAppelantLUrlDirectement() throws Exception {
        Incident incident = creerIncidentAffecte();

        mockMvc.perform(post("/incidents/{id}/resolution", incident.getId()).with(user(agentTerrain())))
                .andExpect(status().isForbidden());

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);
        assertThat(incident.getDateHeureResolution()).isNull();
    }

    @Test
    void resoudreUnIncidentSansPrestataireEstRefuseAvecUnMessageComprehensible() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(post("/incidents/{id}/resolution", incident.getId()).with(user(responsable())))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Affectez d&#39;abord un prestataire")));

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    void leBoutonDeResolutionNEstProposeQuAuResponsableSurUnIncidentAffecte() throws Exception {
        Incident affecte = creerIncidentAffecte();
        Incident nonAffecte = creerIncidentOuvert();
        Incident resolu = creerIncidentResolu();

        mockMvc.perform(get("/incidents/{id}", affecte.getId()).with(user(responsable())))
                .andExpect(content().string(containsString("/resolution")));
        mockMvc.perform(get("/incidents/{id}", affecte.getId()).with(user(agentTerrain())))
                .andExpect(content().string(not(containsString("/resolution"))));
        mockMvc.perform(get("/incidents/{id}", nonAffecte.getId()).with(user(responsable())))
                .andExpect(content().string(not(containsString("/resolution"))));
        mockMvc.perform(get("/incidents/{id}", resolu.getId()).with(user(responsable())))
                .andExpect(content().string(not(containsString("/resolution"))));
    }

    @Test
    void leResponsablePeutAffecterPuisReaffecterUnPrestataire() throws Exception {
        Incident incident = creerIncidentOuvert();
        Prestataire premier = creerPrestataire("Shakira");
        Prestataire second = creerPrestataire("Celine Dion");

        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", premier.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/" + incident.getId()));
        assertThat(incident.getPrestataire()).isEqualTo(premier);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);

        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", second.getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(incident.getPrestataire()).isEqualTo(second);
    }

    @Test
    void unAgentNePeutPasAffecterUnPrestataireMemeEnAppelantLUrlDirectement() throws Exception {
        Incident incident = creerIncidentOuvert();
        Prestataire prestataire = creerPrestataire("Aya Nakamura");

        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(agentTerrain()))
                        .param("prestataireId", prestataire.getId().toString()))
                .andExpect(status().isForbidden());

        assertThat(incident.getPrestataire()).isNull();
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    void seulLeResponsableVoitLeFormulaireDAffectation() throws Exception {
        Incident incident = creerIncidentOuvert();
        creerPrestataire("Angele");

        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Angele")));
        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Angele"))));
    }

    @Test
    void unIncidentResoluNeProposePasLAffectationEtLaRefuseCoteServeur() throws Exception {
        Incident incident = creerIncidentResolu();
        Prestataire prestataire = creerPrestataire("Aya Nakamura");

        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(responsable())))
                .andExpect(content().string(not(containsString("/affectation"))));
        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", prestataire.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Un incident résolu ne peut plus être affecté ni réaffecté.")));

        assertThat(incident.getPrestataire()).isNull();
    }

    @Test
    void uneAffectationSansPrestataireValideAfficheUnMessageComprehensible() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Veuillez choisir un prestataire.")));
        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Veuillez choisir un prestataire.")))
                .andExpect(content().string(not(containsString("Failed to convert"))));

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    void affecterUnPrestataireInexistantDonneUne404() throws Exception {
        Incident incident = creerIncidentOuvert();

        mockMvc.perform(post("/incidents/{id}/affectation", incident.getId()).with(user(responsable()))
                        .param("prestataireId", ID_INEXISTANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("n&#39;existe pas")));

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
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
        mockMvc.perform(get("/equipements/{id}/incidents/nouveau", ID_INEXISTANT).with(user(agentTerrain())))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("n&#39;existe pas")))
                .andExpect(content().string(containsString("Stromae")));
    }
}
