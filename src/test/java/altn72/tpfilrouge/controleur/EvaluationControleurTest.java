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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvaluationControleurTest {

    private static final Long ID_INEXISTANT = 999_999L;
    private static final String TITRE_FORMULAIRE = "Enregistrer l'évaluation";

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
    private Prestataire prestataire;

    @BeforeEach
    void creerDonnees() {
        Station station = stationRepository.save(new Station("Station Test"));
        equipement = equipementRepository.save(
                new Equipement("EQ-TEST-001", "Portique de test", "Portique", "EN_SERVICE", station));
        prestataire = prestataireRepository.save(new Prestataire("Rihanna", "a@b.fr", "0100000000"));
    }

    private UtilisateurDetails responsable() {
        return utilisateur("RESPONSABLE_EXPLOITATION");
    }

    private UtilisateurDetails agentTerrain() {
        return utilisateur("AGENT_TERRAIN");
    }

    private UtilisateurDetails utilisateur(String profil) {
        return new UtilisateurDetails("login", "secret", List.of(new SimpleGrantedAuthority("ROLE_" + profil)), "Stromae", profil);
    }

    private Incident creerIncidentResolu(Prestataire prestataireAffecte) {
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(50), equipement);
        incident.setPrestataire(prestataireAffecte);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        return incidentRepository.save(incident);
    }

    private Incident creerIncidentAffecte() {
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(5), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.AFFECTE);
        return incidentRepository.save(incident);
    }

    @Test
    void leResponsableEvalueUnIncidentResoluPuisVoitLaNoteSurLeDetail() throws Exception {
        Incident incident = creerIncidentResolu(prestataire);

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(responsable()))
                        .param("note", "4")
                        .param("commentaire", "Intervention soignée"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/" + incident.getId()));

        assertThat(evaluationRepository.findByIncidentId(incident.getId())).isPresent();
        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(responsable())))
                .andExpect(content().string(containsString("4 / 5")))
                .andExpect(content().string(containsString("Intervention soignée")))
                .andExpect(content().string(not(containsString(TITRE_FORMULAIRE))));
    }

    @Test
    void leCommentaireEstFacultatif() throws Exception {
        Incident incident = creerIncidentResolu(prestataire);

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(responsable()))
                        .param("note", "5")
                        .param("commentaire", ""))
                .andExpect(status().is3xxRedirection());

        Evaluation evaluation = evaluationRepository.findByIncidentId(incident.getId()).orElseThrow();
        assertThat(evaluation.getCommentaire()).isNull();
    }

    @Test
    void unAgentNePeutPasEvaluerMemeEnAppelantLUrlDirectement() throws Exception {
        Incident incident = creerIncidentResolu(prestataire);

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(agentTerrain()))
                        .param("note", "4"))
                .andExpect(status().isForbidden());

        assertThat(evaluationRepository.findByIncidentId(incident.getId())).isEmpty();
    }

    @Test
    void seulLeResponsableVoitLeFormulaireDEvaluation() throws Exception {
        Incident incident = creerIncidentResolu(prestataire);

        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(responsable())))
                .andExpect(content().string(containsString(TITRE_FORMULAIRE)));
        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TITRE_FORMULAIRE))));
    }

    @Test
    void leFormulaireNEstPasProposeSurUnIncidentNonResolu() throws Exception {
        Incident incident = creerIncidentAffecte();

        mockMvc.perform(get("/incidents/{id}", incident.getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TITRE_FORMULAIRE))));
    }

    @ParameterizedTest
    @CsvSource({
            "0, La note doit être au moins égale à 1.",
            "6, La note ne doit pas dépasser 5.",
            "abc, La note doit être un nombre entier.",
            "'', Veuillez choisir une note."
    })
    void uneNoteInvalideEstRefuseeAvecUnMessageComprehensible(String note, String message) throws Exception {
        Incident incident = creerIncidentResolu(prestataire);

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(responsable()))
                        .param("note", note))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(message)));

        assertThat(evaluationRepository.findByIncidentId(incident.getId())).isEmpty();
    }

    @Test
    void uneSecondeEvaluationEstRefuseeEtLaPremiereEstConservee() throws Exception {
        Incident incident = creerIncidentResolu(prestataire);
        evaluationRepository.save(new Evaluation(2, null, LocalDateTime.now(), incident));

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(responsable()))
                        .param("note", "5"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("déjà été évalué")));

        assertThat(evaluationRepository.findByIncidentId(incident.getId()).orElseThrow().getNote()).isEqualTo(2);
    }

    @Test
    void evaluerUnIncidentNonResoluEstRefuse() throws Exception {
        Incident incident = creerIncidentAffecte();

        mockMvc.perform(post("/incidents/{id}/evaluation", incident.getId()).with(user(responsable()))
                        .param("note", "5"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Seul un incident résolu peut être évalué.")));

        assertThat(evaluationRepository.findByIncidentId(incident.getId())).isEmpty();
    }

    @Test
    void evaluerUnIncidentInexistantDonneUne404() throws Exception {
        mockMvc.perform(post("/incidents/{id}/evaluation", ID_INEXISTANT).with(user(responsable()))
                        .param("note", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uneSaisieInvalideSurLUnDesFormulairesReafficheLeDetailSansErreurServeur() throws Exception {
        mockMvc.perform(post("/incidents/{id}/evaluation", creerIncidentAffecte().getId()).with(user(responsable()))
                        .param("note", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/incidents/{id}/affectation", creerIncidentResolu(prestataire).getId()).with(user(responsable()))
                        .param("prestataireId", ""))
                .andExpect(status().isOk());
    }

    @Test
    void lHistoriqueAfficheLaNoteOuSignaleLAbsenceDEvaluation() throws Exception {
        Incident evalue = creerIncidentResolu(prestataire);
        evaluationRepository.save(new Evaluation(3, null, LocalDateTime.now(), evalue));
        creerIncidentResolu(prestataire);

        mockMvc.perform(get("/incidents/historique").with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3 / 5")))
                .andExpect(content().string(containsString("Non évalué")));
    }

    @Test
    void laListeDesPrestatairesAfficheLaMoyenneEtLeNombreDEvaluations() throws Exception {
        Prestataire sansEvaluation = prestataireRepository.save(new Prestataire("Zaho", "z@b.fr", "0100000001"));
        evaluationRepository.save(new Evaluation(4, null, LocalDateTime.now(), creerIncidentResolu(prestataire)));
        evaluationRepository.save(new Evaluation(5, null, LocalDateTime.now(), creerIncidentResolu(prestataire)));
        creerIncidentAffecte();

        mockMvc.perform(get("/prestataires").with(user(agentTerrain())).locale(Locale.FRENCH))
                .andExpect(status().isOk())
                .andExpect(content().string(stringContainsInOrder(
                        prestataire.getNom(), "4,5 / 5", ">2<", sansEvaluation.getNom(), "Pas encore évalué", ">0<")));
    }

    @Test
    void laListeDesPrestatairesDemandeUneConnexion() throws Exception {
        mockMvc.perform(get("/prestataires"))
                .andExpect(status().is3xxRedirection());
    }
}
