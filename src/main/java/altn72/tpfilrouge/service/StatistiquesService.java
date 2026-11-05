package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.IncidentsParStation;
import altn72.tpfilrouge.modele.NombreIncidentsParType;
import altn72.tpfilrouge.modele.NotePrestataire;
import altn72.tpfilrouge.modele.SerieStatistique;
import altn72.tpfilrouge.modele.StatistiquesIncidents;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatistiquesService {

    private static final int NOMBRE_JOURS_TENDANCE = 14;
    private static final int DECIMALES_AFFICHEES = 1;
    private static final double MINUTES_PAR_HEURE = 60.0;
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("dd/MM");

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final PrestataireService prestataireService;
    private final Clock horloge;

    public StatistiquesService(IncidentRepository incidentRepository,
                               IncidentService incidentService,
                               PrestataireService prestataireService,
                               Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.prestataireService = prestataireService;
        this.horloge = horloge;
    }

    public StatistiquesIncidents calculerStatistiques() {
        List<IncidentsParStation> incidentsParStation = incidentRepository.compterIncidentsParStation();
        return new StatistiquesIncidents(
                extraire(incidentsParStation, IncidentsParStation::getStation),
                extraire(incidentsParStation, IncidentsParStation::getNombreActifs),
                extraire(incidentsParStation, IncidentsParStation::getNombreResolus),
                calculerIncidentsParType(),
                calculerOuverturesParJour(),
                calculerNotesParPrestataire(),
                calculerDelaisResolutionParPrestataire());
    }

    private <T, R> List<R> extraire(List<T> elements, Function<T, R> extracteur) {
        return elements.stream().map(extracteur).toList();
    }

    private SerieStatistique calculerIncidentsParType() {
        List<NombreIncidentsParType> incidentsParType = incidentRepository.compterIncidentsParType();
        return new SerieStatistique(
                extraire(incidentsParType, NombreIncidentsParType::type),
                extraire(incidentsParType, NombreIncidentsParType::nombre));
    }

    private SerieStatistique calculerOuverturesParJour() {
        LocalDate premierJour = LocalDate.now(horloge).minusDays(NOMBRE_JOURS_TENDANCE - 1L);
        Map<LocalDate, Long> ouvertures = incidentRepository
                .findByDateHeureOuvertureGreaterThanEqual(premierJour.atStartOfDay()).stream()
                .collect(Collectors.groupingBy(incident -> incident.getDateHeureOuverture().toLocalDate(), Collectors.counting()));
        // Une tendance doit aussi montrer les jours sans incident : ils sont absents du résultat de la requête.
        List<LocalDate> jours = premierJour.datesUntil(premierJour.plusDays(NOMBRE_JOURS_TENDANCE)).toList();
        return new SerieStatistique(
                extraire(jours, FORMAT_JOUR::format),
                extraire(jours, jour -> ouvertures.getOrDefault(jour, 0L)));
    }

    private SerieStatistique calculerNotesParPrestataire() {
        List<NotePrestataire> notes = prestataireService.obtenirNotesDesPrestataires().stream()
                .filter(note -> note.noteMoyenne() != null)
                .toList();
        return new SerieStatistique(
                extraire(notes, note -> note.prestataire().getNom()),
                extraire(notes, note -> arrondir(note.noteMoyenne())));
    }

    private SerieStatistique calculerDelaisResolutionParPrestataire() {
        Map<String, Double> minutesParPrestataire = incidentService.obtenirHistoriqueDesIncidentsResolus().stream()
                .filter(incident -> incident.getPrestataire() != null)
                .collect(Collectors.groupingBy(
                        incident -> incident.getPrestataire().getNom(),
                        TreeMap::new,
                        Collectors.averagingLong(this::calculerMinutesOuverture)));
        return new SerieStatistique(
                List.copyOf(minutesParPrestataire.keySet()),
                extraire(List.copyOf(minutesParPrestataire.values()), minutes -> arrondir(minutes / MINUTES_PAR_HEURE)));
    }

    private long calculerMinutesOuverture(Incident incident) {
        return incidentService.calculerDureeOuverture(incident).toMinutes();
    }

    private BigDecimal arrondir(double valeur) {
        return BigDecimal.valueOf(valeur).setScale(DECIMALES_AFFICHEES, RoundingMode.HALF_UP);
    }
}
