package altn72.tpfilrouge.modele;

import java.util.List;

public record SerieStatistique(List<String> libelles, List<? extends Number> valeurs) {
}
