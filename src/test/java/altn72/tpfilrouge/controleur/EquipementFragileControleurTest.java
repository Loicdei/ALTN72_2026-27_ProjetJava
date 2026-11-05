package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.LigneTableauDeBord;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.modele.TableauDeBord;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipementFragileControleurTest {

    private static final String BADGE = "Équipement fragile";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private Equipement fragile;
    private Equipement voisin;

    @BeforeEach
    void creerDonnees() {
        Station station = stationRepository.save(new Station("Station Test"));
        fragile = equipementRepository.save(new Equipement("EQ-FRAGILE", "Portique", "Portique", "EN_SERVICE", station));
        voisin = equipementRepository.save(new Equipement("EQ-VOISIN", "Écran", "Écran", "EN_SERVICE", station));
    }

    private UtilisateurDetails responsable() {
        String profil = "RESPONSABLE_EXPLOITATION";
        return new UtilisateurDetails("login", "secret", List.of(new SimpleGrantedAuthority("ROLE_" + profil)), "Stromae", profil);
    }

    private void ouvrirIncidents(Equipement concerne, int nombre) {
        for (int i = 0; i < nombre; i++) {
            incidentRepository.save(new Incident("Panne de " + concerne.getReference(), LocalDateTime.now().minusHours(i + 1L), concerne));
        }
    }

    @Test
    void laFicheAfficheLeBadgeSiLEquipementEstFragile() throws Exception {
        ouvrirIncidents(fragile, 3);

        mockMvc.perform(get("/equipements/{id}", fragile.getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(BADGE)));
    }

    @Test
    void laFicheNAffichePasLeBadgeSiLEquipementNEstPasFragile() throws Exception {
        ouvrirIncidents(fragile, 3);
        ouvrirIncidents(voisin, 2);

        mockMvc.perform(get("/equipements/{id}", voisin.getId()).with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(BADGE))));
    }

    @Test
    void leTableauDeBordMarqueSeulementLesLignesDesEquipementsFragiles() throws Exception {
        ouvrirIncidents(fragile, 3);
        ouvrirIncidents(voisin, 1);

        TableauDeBord tableau = (TableauDeBord) mockMvc.perform(get("/tableau-de-bord").with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(BADGE)))
                .andReturn().getModelAndView().getModel().get("tableau");

        assertThat(tableau.lignes()).hasSize(4);
        for (LigneTableauDeBord ligne : tableau.lignes()) {
            boolean surLEquipementFragile = ligne.incident().getEquipement().getId().equals(fragile.getId());
            assertThat(ligne.equipementFragile()).isEqualTo(surLEquipementFragile);
        }
    }

    @Test
    void sansEquipementFragileLeTableauDeBordNAffichePasLeBadge() throws Exception {
        ouvrirIncidents(voisin, 2);

        mockMvc.perform(get("/tableau-de-bord").with(user(responsable())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(BADGE))));
    }

    @Test
    void laTroisiemeDeclarationPrevientQueLEquipementEstFragile() throws Exception {
        ouvrirIncidents(fragile, 2);

        mockMvc.perform(post("/equipements/{id}/incidents", fragile.getId()).with(user(responsable()))
                        .param("description", "Troisième panne"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("messageSucces", containsString("fragile")));
    }

    @Test
    void uneDeclarationSansFragiliteNeMentionneRien() throws Exception {
        ouvrirIncidents(voisin, 1);

        mockMvc.perform(post("/equipements/{id}/incidents", voisin.getId()).with(user(responsable()))
                        .param("description", "Deuxième panne"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("messageSucces", not(containsString("fragile"))));
    }
}
