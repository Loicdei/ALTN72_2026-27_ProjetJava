package altn72.tpfilrouge.modele;

import java.time.Duration;
import java.util.List;

public record TableauDeBord(List<LigneTableauDeBord> lignes,
                            List<CompteurNiveauAttention> compteurs,
                            long nombreIncidentsActifs,
                            Duration attenteMaximaleSansPrestataire) {
}
