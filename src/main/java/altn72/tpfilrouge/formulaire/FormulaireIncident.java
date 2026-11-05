package altn72.tpfilrouge.formulaire;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FormulaireIncident {

    private static final int LONGUEUR_MAX_DESCRIPTION = 2000;

    @NotBlank(message = "La description est obligatoire.")
    @Size(max = LONGUEUR_MAX_DESCRIPTION, message = "La description ne doit pas dépasser {max} caractères.")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
