package altn72.tpfilrouge.modele;

import java.util.List;

public record StatistiquesIncidents(List<String> stations,
                                    List<Long> actifsParStation,
                                    List<Long> resolusParStation,
                                    SerieStatistique incidentsParType,
                                    SerieStatistique ouverturesParJour,
                                    SerieStatistique notesParPrestataire,
                                    SerieStatistique delaisResolutionParPrestataire) {
}
