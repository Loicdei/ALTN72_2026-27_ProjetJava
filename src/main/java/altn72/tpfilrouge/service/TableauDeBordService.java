package altn72.tpfilrouge.service;

import altn72.tpfilrouge.formulaire.FiltreTableauDeBord;
import altn72.tpfilrouge.modele.CompteurNiveauAttention;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.LigneTableauDeBord;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.modele.TableauDeBord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class TableauDeBordService {

    private static final Comparator<LigneTableauDeBord> ORDRE_D_URGENCE = Comparator
            .comparing(LigneTableauDeBord::niveauAttention)
            .thenComparing(LigneTableauDeBord::dureeOuverture, Comparator.reverseOrder());

    private final IncidentService incidentService;

    public TableauDeBordService(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    public TableauDeBord construireTableauDeBord(FiltreTableauDeBord filtre) {
        List<LigneTableauDeBord> lignes = incidentService.obtenirIncidentsActifs(filtre.stationId(), filtre.type()).stream()
                .map(this::creerLigne)
                .sorted(ORDRE_D_URGENCE)
                .toList();
        // Le niveau est calculé à l'instant de la lecture : ce filtre ne peut donc pas être fait par la base.
        // Les compteurs l'ignorent pour continuer à montrer la répartition quand on clique sur un niveau.
        return new TableauDeBord(
                filtrerParNiveau(lignes, filtre.niveau()),
                compterParNiveau(lignes),
                lignes.size(),
                calculerAttenteMaximaleSansPrestataire(lignes));
    }

    private LigneTableauDeBord creerLigne(Incident incident) {
        Duration dureeOuverture = incidentService.calculerDureeOuverture(incident);
        NiveauAttention niveau = NiveauAttention.determinerPourIncidentActif(incident.getStatut(), dureeOuverture);
        return new LigneTableauDeBord(incident, niveau, dureeOuverture);
    }

    private List<LigneTableauDeBord> filtrerParNiveau(List<LigneTableauDeBord> lignes, NiveauAttention niveau) {
        if (niveau == null) {
            return lignes;
        }
        return lignes.stream().filter(ligne -> ligne.niveauAttention() == niveau).toList();
    }

    private List<CompteurNiveauAttention> compterParNiveau(List<LigneTableauDeBord> lignes) {
        return Arrays.stream(NiveauAttention.values())
                .map(niveau -> new CompteurNiveauAttention(niveau,
                        lignes.stream().filter(ligne -> ligne.niveauAttention() == niveau).count()))
                .toList();
    }

    private Duration calculerAttenteMaximaleSansPrestataire(List<LigneTableauDeBord> lignes) {
        return lignes.stream()
                .filter(ligne -> ligne.niveauAttention() != NiveauAttention.PRIS_EN_CHARGE)
                .map(LigneTableauDeBord::dureeOuverture)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
