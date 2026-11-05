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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportExportControleurTest {

    private static final String ROUTE_EXPORT = "/incidents/historique/export";
    private static final String ROUTE_IMPORT = "/equipements/import";
    private static final String ENTETE_IMPORT = "reference;libelle;type;etat;station\n";

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
        Station station = stationRepository.save(new Station("Station Nord"));
        equipement = equipementRepository.save(new Equipement("EQ-EXISTANT", "Portique", "Portique", "EN_SERVICE", station));
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

    private Incident creerIncidentResolu(String description, int heuresDepuisResolution) {
        Incident incident = new Incident(description, LocalDateTime.now().minusDays(5), equipement);
        incident.setPrestataire(prestataire);
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateHeureResolution(LocalDateTime.now().minusHours(heuresDepuisResolution));
        return incidentRepository.save(incident);
    }

    private String exporter() throws Exception {
        return mockMvc.perform(get(ROUTE_EXPORT).with(user(agentTerrain())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MockMultipartFile fichier(String contenu) {
        return new MockMultipartFile("fichier", "equipements.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void lExportEstUnFichierCsvATelecharger() throws Exception {
        mockMvc.perform(get(ROUTE_EXPORT).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("historique-incidents.csv")));
    }

    @Test
    void lExportEstOrdonneDuPlusRecentAuPlusAncienAvecLaNote() throws Exception {
        Incident ancien = creerIncidentResolu("Résolu il y a longtemps", 30);
        creerIncidentResolu("Résolu récemment", 2);
        evaluationRepository.save(new Evaluation(4, null, LocalDateTime.now(), ancien));

        List<String> lignes = exporter().lines().toList();

        assertThat(lignes).hasSize(3);
        assertThat(lignes.get(0)).startsWith("﻿Résolu le;Ouvert le;");
        assertThat(lignes.get(1)).contains("Résolu récemment;EQ-EXISTANT;Station Nord;Rihanna;").endsWith(";");
        assertThat(lignes.get(2)).contains("Résolu il y a longtemps").endsWith(";4");
    }

    @Test
    void lExportNeContientQueLesIncidentsResolus() throws Exception {
        incidentRepository.save(new Incident("Encore ouvert", LocalDateTime.now(), equipement));

        assertThat(exporter()).doesNotContain("Encore ouvert");
    }

    @Test
    void lesSeparateursDansUneDescriptionSontProteges() throws Exception {
        creerIncidentResolu("Panne; porte \"nord\"", 1);

        assertThat(exporter()).contains("\"Panne; porte \"\"nord\"\"\"");
    }

    @Test
    void uneDescriptionQuiRessembleAUneFormuleEstNeutralisee() throws Exception {
        creerIncidentResolu("=HYPERLINK(\"http://exemple.fr\")", 1);

        assertThat(exporter()).contains("'=HYPERLINK").doesNotContain(";=HYPERLINK").doesNotContain(";\"=HYPERLINK");
    }

    @Test
    void lExportDemandeUneConnexion() throws Exception {
        mockMvc.perform(get(ROUTE_EXPORT)).andExpect(status().is3xxRedirection());
    }

    @Test
    void lHistoriqueProposeLExportSeulementSIlYADesIncidents() throws Exception {
        mockMvc.perform(get("/incidents/historique").with(user(agentTerrain())))
                .andExpect(content().string(not(containsString(ROUTE_EXPORT))));

        creerIncidentResolu("Résolu", 1);

        mockMvc.perform(get("/incidents/historique").with(user(agentTerrain())))
                .andExpect(content().string(containsString(ROUTE_EXPORT)));
    }

    @Test
    void leResponsableImporteDesEquipementsPuisRevientALaListe() throws Exception {
        String csv = ENTETE_IMPORT + "EQ-NOUVEAU;Ascenseur;Ascenseur;EN_PANNE;Station Nord\n";

        mockMvc.perform(multipart(ROUTE_IMPORT).file(fichier(csv)).with(user(responsable())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipements"))
                .andExpect(flash().attribute("messageSucces", "Équipements importés : 1."));

        assertThat(equipementRepository.existsByReference("EQ-NOUVEAU")).isTrue();
    }

    @Test
    void unFichierAvecUneLigneInvalideNImporteRienEtAfficheLErreur() throws Exception {
        String csv = ENTETE_IMPORT
                + "EQ-VALIDE;Ascenseur;Ascenseur;EN_SERVICE;Station Nord\n"
                + "EQ-EXISTANT;Doublon;Portique;EN_SERVICE;Station Nord\n";

        mockMvc.perform(multipart(ROUTE_IMPORT).file(fichier(csv)).with(user(responsable())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ligne 3 : la référence « EQ-EXISTANT » existe déjà.")))
                .andExpect(content().string(containsString("Aucun équipement n'a été importé")))
                .andExpect(content().string(containsString("Stromae")));

        assertThat(equipementRepository.existsByReference("EQ-VALIDE")).isFalse();
    }

    @Test
    void unAgentNePeutPasImporterMemeEnAppelantLUrlDirectement() throws Exception {
        String csv = ENTETE_IMPORT + "EQ-NOUVEAU;Ascenseur;Ascenseur;EN_SERVICE;Station Nord\n";

        mockMvc.perform(multipart(ROUTE_IMPORT).file(fichier(csv)).with(user(agentTerrain())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ROUTE_IMPORT).with(user(agentTerrain())))
                .andExpect(status().isForbidden());

        assertThat(equipementRepository.existsByReference("EQ-NOUVEAU")).isFalse();
    }

    @Test
    void seulLeResponsableVoitLeLienDImport() throws Exception {
        mockMvc.perform(get("/equipements").with(user(responsable())))
                .andExpect(content().string(containsString(ROUTE_IMPORT)));
        mockMvc.perform(get("/equipements").with(user(agentTerrain())))
                .andExpect(content().string(not(containsString(ROUTE_IMPORT))));
    }

    @Test
    void leFormulaireDImportDecritLeFormatAttendu() throws Exception {
        mockMvc.perform(get(ROUTE_IMPORT).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("reference;libelle;type;etat;station")));
    }
}
