package altn72.tpfilrouge.api;

import jakarta.validation.constraints.NotNull;

public record AffectationRequete(@NotNull(message = "L'identifiant du prestataire est obligatoire.") Long prestataireId) {
}
