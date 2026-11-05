package altn72.tpfilrouge.api;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.modele.StatutIncident;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDateTime;

public record IncidentReponse(
        Long id,
        String description,
        StatutIncident statut,
        @Schema(description = "Niveau calculé à l'instant de la requête, absent si l'incident est résolu")
        NiveauAttention niveauAttention,
        LocalDateTime dateHeureOuverture,
        LocalDateTime dateHeureResolution,
        @Schema(description = "Durée d'ouverture en minutes, jusqu'à la résolution si l'incident est résolu")
        long dureeOuvertureMinutes,
        Long equipementId,
        String equipementReference,
        String station,
        Long prestataireId,
        String prestataireNom) {

    public static IncidentReponse depuis(Incident incident, Duration dureeOuverture, NiveauAttention niveauAttention) {
        Equipement equipement = incident.getEquipement();
        Prestataire prestataire = incident.getPrestataire();
        return new IncidentReponse(
                incident.getId(),
                incident.getDescription(),
                incident.getStatut(),
                niveauAttention,
                incident.getDateHeureOuverture(),
                incident.getDateHeureResolution(),
                dureeOuverture.toMinutes(),
                equipement.getId(),
                equipement.getReference(),
                equipement.getStation().getNom(),
                prestataire != null ? prestataire.getId() : null,
                prestataire != null ? prestataire.getNom() : null);
    }
}
