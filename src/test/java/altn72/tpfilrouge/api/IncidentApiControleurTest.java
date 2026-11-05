package altn72.tpfilrouge.api;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.StatutIncident;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentApiControleurTest {

    private static final String ROUTE = "/api/incidents";
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

    private Equipement equipement;
    private Prestataire prestataire;

    @BeforeEach
    void creerDonnees() {
        Station station = stationRepository.save(new Station("Station Test"));
        equipement = equipementRepository.save(
                new Equipement("EQ-TEST-001", "Portique de test", "Portique", "EN_SERVICE", station));
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

    private Incident creerIncidentNonAffecte(int heures) {
        return incidentRepository.save(new Incident("Portique bloqué", LocalDateTime.now().minusHours(heures), equipement));
    }

    private Incident creerIncidentAffecte() {
        Incident incident = new Incident("Portique bloqué", LocalDateTime.now().minusHours(5), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.AFFECTE);
        return incidentRepository.save(incident);
    }

    private Incident creerIncidentResolu() {
        Incident incident = creerIncidentAffecte();
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(1));
        return incident;
    }

    private String corpsAffectation(Object prestataireId) {
        return "{\"prestataireId\": " + prestataireId + "}";
    }

    @Test
    void unUtilisateurNonConnecteEstRenvoyeVersLaConnexion() throws Exception {
        mockMvc.perform(get(ROUTE)).andExpect(status().is3xxRedirection());
    }

    @Test
    void consulterUnIncidentRenvoieSesInformationsEnJson() throws Exception {
        Incident incident = creerIncidentNonAffecte(50);

        mockMvc.perform(get(ROUTE + "/{id}", incident.getId()).with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.statut").value("NON_AFFECTE"))
                .andExpect(jsonPath("$.niveauAttention").value("ORANGE"))
                .andExpect(jsonPath("$.equipementReference").value("EQ-TEST-001"))
                .andExpect(jsonPath("$.station").value("Station Test"))
                .andExpect(jsonPath("$.prestataireNom").value(nullValue()));
    }

    @Test
    void unIncidentResoluNaPasDeNiveauDAttention() throws Exception {
        mockMvc.perform(get(ROUTE + "/{id}", creerIncidentResolu().getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESOLU"))
                .andExpect(jsonPath("$.niveauAttention").value(nullValue()));
    }

    @Test
    void unIncidentInexistantDonneUne404EnJsonAvecUnMessageEnFrancais() throws Exception {
        mockMvc.perform(get(ROUTE + "/{id}", ID_INEXISTANT).with(user(responsable())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("L'incident n° " + ID_INEXISTANT + " n'existe pas."));
    }

    @Test
    void unIdentifiantMalFormeDonneUne400() throws Exception {
        mockMvc.perform(get(ROUTE + "/abc").with(user(responsable())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void laListeRenvoieTousLesIncidentsDuPlusRecentAuPlusAncien() throws Exception {
        Incident ancien = creerIncidentNonAffecte(30);
        Incident recent = creerIncidentNonAffecte(2);

        mockMvc.perform(get(ROUTE).with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(recent.getId()))
                .andExpect(jsonPath("$[1].id").value(ancien.getId()));
    }

    @Test
    void laListeSeFiltreParStatut() throws Exception {
        creerIncidentNonAffecte(1);
        Incident resolu = creerIncidentResolu();

        mockMvc.perform(get(ROUTE).param("statut", "RESOLU").with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(resolu.getId()));
    }

    @Test
    void uneListeSansResultatRenvoieUn200AvecUnTableauVide() throws Exception {
        mockMvc.perform(get(ROUTE).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unStatutInconnuDonneUne400() throws Exception {
        mockMvc.perform(get(ROUTE).param("statut", "VIOLET").with(user(responsable())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void leResponsableAffecteUnPrestataire() throws Exception {
        Incident incident = creerIncidentNonAffecte(1);

        mockMvc.perform(put(ROUTE + "/{id}/prestataire", incident.getId()).with(user(responsable()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAffectation(prestataire.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("AFFECTE"))
                .andExpect(jsonPath("$.niveauAttention").value("PRIS_EN_CHARGE"))
                .andExpect(jsonPath("$.prestataireNom").value("Rihanna"));
    }

    @Test
    void unAgentNePeutPasAffecterUnPrestataire() throws Exception {
        Incident incident = creerIncidentNonAffecte(1);

        mockMvc.perform(put(ROUTE + "/{id}/prestataire", incident.getId()).with(user(agentTerrain()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAffectation(prestataire.getId())))
                .andExpect(status().isForbidden());

        assertThat(incident.getPrestataire()).isNull();
    }

    @Test
    void uneAffectationSansPrestataireDonneUne400AvecUnMessageClair() throws Exception {
        mockMvc.perform(put(ROUTE + "/{id}/prestataire", creerIncidentNonAffecte(1).getId()).with(user(responsable()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAffectation(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("L'identifiant du prestataire est obligatoire."));
    }

    @Test
    void unCorpsIllisibleDonneUne400() throws Exception {
        mockMvc.perform(put(ROUTE + "/{id}/prestataire", creerIncidentNonAffecte(1).getId()).with(user(responsable()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le corps de la requête n'est pas un JSON valide."));
    }

    @Test
    void affecterUnPrestataireInexistantDonneUne404() throws Exception {
        mockMvc.perform(put(ROUTE + "/{id}/prestataire", creerIncidentNonAffecte(1).getId()).with(user(responsable()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAffectation(ID_INEXISTANT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void affecterUnIncidentResoluDonneUne409() throws Exception {
        mockMvc.perform(put(ROUTE + "/{id}/prestataire", creerIncidentResolu().getId()).with(user(responsable()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAffectation(prestataire.getId())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void leResponsableDeclareResoluUnIncidentAffecte() throws Exception {
        mockMvc.perform(post(ROUTE + "/{id}/resolution", creerIncidentAffecte().getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESOLU"))
                .andExpect(jsonPath("$.dateHeureResolution").isNotEmpty());
    }

    @Test
    void unAgentNePeutPasDeclarerResolu() throws Exception {
        Incident incident = creerIncidentAffecte();

        mockMvc.perform(post(ROUTE + "/{id}/resolution", incident.getId()).with(user(agentTerrain())))
                .andExpect(status().isForbidden());

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);
    }

    @Test
    void resoudreUnIncidentSansPrestataireDonneUne409() throws Exception {
        mockMvc.perform(post(ROUTE + "/{id}/resolution", creerIncidentNonAffecte(1).getId()).with(user(responsable())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Affectez d'abord un prestataire pour pouvoir résoudre cet incident."));
    }

    @Test
    void resoudreUnIncidentDejaResoluDonneUne409() throws Exception {
        mockMvc.perform(post(ROUTE + "/{id}/resolution", creerIncidentResolu().getId()).with(user(responsable())))
                .andExpect(status().isConflict());
    }

    @Test
    void resoudreUnIncidentInexistantDonneUne404() throws Exception {
        mockMvc.perform(post(ROUTE + "/{id}/resolution", ID_INEXISTANT).with(user(responsable())))
                .andExpect(status().isNotFound());
    }

    @Test
    void lesPagesWebGardentLeurPageDErreurHtml() throws Exception {
        mockMvc.perform(get("/incidents/{id}", ID_INEXISTANT).with(user(responsable())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void laDocumentationOpenApiDecritLApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs").with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de supervision des incidents"))
                .andExpect(jsonPath("$.paths['/api/incidents/{id}/resolution'].post.summary").value("Déclarer un incident résolu"));
    }

    @Test
    void laDocumentationDemandeUneConnexion() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().is3xxRedirection());
    }
}
