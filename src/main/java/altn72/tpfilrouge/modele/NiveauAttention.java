package altn72.tpfilrouge.modele;

import java.time.Duration;

// L'ordre de déclaration est celui de l'urgence : le tableau de bord s'en sert pour trier les incidents.
public enum NiveauAttention {

    ROUGE("Rouge"),
    ORANGE("Orange"),
    VERT("Vert"),
    PRIS_EN_CHARGE("Pris en charge");

    private static final Duration SEUIL_ORANGE = Duration.ofHours(48);
    private static final Duration SEUIL_ROUGE = Duration.ofHours(72);

    private final String libelle;

    NiveauAttention(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static NiveauAttention determinerPourIncidentActif(StatutIncident statut, Duration dureeOuverture) {
        if (statut == StatutIncident.AFFECTE) {
            return PRIS_EN_CHARGE;
        }
        if (dureeOuverture.compareTo(SEUIL_ROUGE) >= 0) {
            return ROUGE;
        }
        if (dureeOuverture.compareTo(SEUIL_ORANGE) >= 0) {
            return ORANGE;
        }
        return VERT;
    }
}
