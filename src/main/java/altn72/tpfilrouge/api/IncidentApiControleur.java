package altn72.tpfilrouge.api;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.StatutIncident;
import altn72.tpfilrouge.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Consultation des incidents et changements de leur état")
public class IncidentApiControleur {

    private final IncidentService incidentService;

    public IncidentApiControleur(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(summary = "Lister les incidents", description = "Du plus récemment ouvert au plus ancien.")
    @ApiResponse(responseCode = "200", description = "Liste des incidents, éventuellement vide")
    @ApiResponse(responseCode = "400", description = "Statut inconnu", content = @Content)
    public List<IncidentReponse> listerIncidents(
            @Parameter(description = "Ne garder que les incidents de ce statut")
            @RequestParam(required = false) StatutIncident statut) {
        return incidentService.obtenirIncidents(statut).stream().map(this::convertir).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un incident")
    @ApiResponse(responseCode = "200", description = "Incident trouvé")
    @ApiResponse(responseCode = "400", description = "Identifiant mal formé", content = @Content)
    @ApiResponse(responseCode = "404", description = "Incident inexistant", content = @Content)
    public IncidentReponse consulterIncident(@PathVariable Long id) {
        return convertir(incidentService.obtenirParId(id));
    }

    @PutMapping("/{id}/prestataire")
    @Operation(summary = "Affecter ou réaffecter un prestataire", description = "Réservé au responsable d'exploitation.")
    @ApiResponse(responseCode = "200", description = "Incident affecté")
    @ApiResponse(responseCode = "400", description = "Corps de requête invalide", content = @Content)
    @ApiResponse(responseCode = "403", description = "Profil non autorisé", content = @Content)
    @ApiResponse(responseCode = "404", description = "Incident ou prestataire inexistant", content = @Content)
    @ApiResponse(responseCode = "409", description = "Incident déjà résolu", content = @Content)
    public IncidentReponse affecterPrestataire(@PathVariable Long id, @Valid @RequestBody AffectationRequete requete) {
        return convertir(incidentService.affecterPrestataire(id, requete.prestataireId()));
    }

    @PostMapping("/{id}/resolution")
    @Operation(summary = "Déclarer un incident résolu", description = "Réservé au responsable d'exploitation. "
            + "La date de résolution est celle de la requête.")
    @ApiResponse(responseCode = "200", description = "Incident résolu")
    @ApiResponse(responseCode = "403", description = "Profil non autorisé", content = @Content)
    @ApiResponse(responseCode = "404", description = "Incident inexistant", content = @Content)
    @ApiResponse(responseCode = "409", description = "Incident déjà résolu ou sans prestataire", content = @Content)
    public IncidentReponse declarerResolu(@PathVariable Long id) {
        return convertir(incidentService.declarerResolu(id));
    }

    private IncidentReponse convertir(Incident incident) {
        return IncidentReponse.depuis(incident,
                incidentService.calculerDureeOuverture(incident),
                incidentService.calculerNiveauAttention(incident).orElse(null));
    }
}
