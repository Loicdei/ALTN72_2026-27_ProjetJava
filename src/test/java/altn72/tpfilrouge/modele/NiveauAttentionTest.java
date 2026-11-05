package altn72.tpfilrouge.modele;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class NiveauAttentionTest {

    @ParameterizedTest
    @CsvSource({
            "PT0S, VERT",
            "PT47H, VERT",
            "PT47H59M59S, VERT",
            "PT48H, ORANGE",
            "PT71H59M59S, ORANGE",
            "PT72H, ROUGE",
            "PT200H, ROUGE"
    })
    void unIncidentNonAffecteChangeDeNiveauAuxSeuilsDe48EtDe72Heures(Duration dureeOuverture, NiveauAttention attendu) {
        assertThat(NiveauAttention.determinerPourIncidentActif(StatutIncident.NON_AFFECTE, dureeOuverture))
                .isEqualTo(attendu);
    }

    @ParameterizedTest
    @CsvSource({"PT1H", "PT50H", "PT200H"})
    void unIncidentAffecteEstPrisEnChargeQuelleQueSoitSaDuree(Duration dureeOuverture) {
        assertThat(NiveauAttention.determinerPourIncidentActif(StatutIncident.AFFECTE, dureeOuverture))
                .isEqualTo(NiveauAttention.PRIS_EN_CHARGE);
    }

    @Test
    void lOrdreDeDeclarationVaDuPlusUrgentAuMoinsUrgent() {
        assertThat(NiveauAttention.values()).containsExactly(
                NiveauAttention.ROUGE, NiveauAttention.ORANGE, NiveauAttention.VERT, NiveauAttention.PRIS_EN_CHARGE);
    }
}
