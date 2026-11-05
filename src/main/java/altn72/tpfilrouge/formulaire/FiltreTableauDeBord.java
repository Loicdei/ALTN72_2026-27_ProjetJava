package altn72.tpfilrouge.formulaire;

import altn72.tpfilrouge.modele.NiveauAttention;
import org.springframework.util.StringUtils;

public record FiltreTableauDeBord(Long stationId, String type, NiveauAttention niveau) {

    public FiltreTableauDeBord {
        // L'option « tous les types » envoie une valeur vide, qui ne doit pas être recherchée comme un type.
        type = StringUtils.hasText(type) ? type : null;
    }
}
