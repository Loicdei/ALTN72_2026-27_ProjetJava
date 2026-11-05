package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Incident;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportHistoriqueService {

    private static final String[] ENTETES = {
            "Résolu le", "Ouvert le", "Durée (minutes)", "Description", "Équipement", "Station", "Prestataire", "Note"
    };
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DEBUTS_DE_FORMULE = "=+-@\t\r";
    private static final String NEUTRALISATION_FORMULE = "'";

    private final IncidentService incidentService;
    private final EvaluationService evaluationService;

    public ExportHistoriqueService(IncidentService incidentService, EvaluationService evaluationService) {
        this.incidentService = incidentService;
        this.evaluationService = evaluationService;
    }

    public String exporterHistorique() {
        List<Incident> incidents = incidentService.obtenirHistoriqueDesIncidentsResolus();
        Map<Long, Integer> notes = evaluationService.obtenirNotesParIncident(incidents);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(FormatCsv.SEPARATEUR)
                .setHeader(ENTETES)
                .get();
        StringWriter contenu = new StringWriter().append(FormatCsv.MARQUE_UTF8);
        try (CSVPrinter imprimante = new CSVPrinter(contenu, format)) {
            for (Incident incident : incidents) {
                imprimante.printRecord(creerLigne(incident, notes.get(incident.getId())));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return contenu.toString();
    }

    private List<Object> creerLigne(Incident incident, Integer note) {
        return List.of(
                FORMAT_DATE.format(incident.getDateHeureResolution()),
                FORMAT_DATE.format(incident.getDateHeureOuverture()),
                incidentService.calculerDureeOuverture(incident).toMinutes(),
                neutraliserFormule(incident.getDescription()),
                neutraliserFormule(incident.getEquipement().getReference()),
                neutraliserFormule(incident.getEquipement().getStation().getNom()),
                incident.getPrestataire() != null ? neutraliserFormule(incident.getPrestataire().getNom()) : "",
                note != null ? note : "");
    }

    // Un tableur exécuterait comme une formule un texte saisi qui commence par l'un de ces caractères.
    private String neutraliserFormule(String texte) {
        boolean ressembleAUneFormule = !texte.isEmpty() && DEBUTS_DE_FORMULE.indexOf(texte.charAt(0)) >= 0;
        return ressembleAUneFormule ? NEUTRALISATION_FORMULE + texte : texte;
    }
}
