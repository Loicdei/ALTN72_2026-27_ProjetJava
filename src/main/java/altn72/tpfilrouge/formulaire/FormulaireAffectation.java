package altn72.tpfilrouge.formulaire;

import jakarta.validation.constraints.NotNull;

public class FormulaireAffectation {

    @NotNull(message = "Veuillez choisir un prestataire.")
    private Long prestataireId;

    public Long getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(Long prestataireId) {
        this.prestataireId = prestataireId;
    }
}
