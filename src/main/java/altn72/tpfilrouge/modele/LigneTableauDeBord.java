package altn72.tpfilrouge.modele;

import java.time.Duration;

public record LigneTableauDeBord(Incident incident, NiveauAttention niveauAttention, Duration dureeOuverture) {
}
