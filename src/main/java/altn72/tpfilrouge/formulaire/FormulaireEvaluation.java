package altn72.tpfilrouge.formulaire;

import altn72.tpfilrouge.modele.Evaluation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FormulaireEvaluation {

    private static final int LONGUEUR_MAX_COMMENTAIRE = 2000;

    @NotNull(message = "Veuillez choisir une note.")
    @Min(value = Evaluation.NOTE_MINIMALE, message = "La note doit être au moins égale à {value}.")
    @Max(value = Evaluation.NOTE_MAXIMALE, message = "La note ne doit pas dépasser {value}.")
    private Integer note;

    @Size(max = LONGUEUR_MAX_COMMENTAIRE, message = "Le commentaire ne doit pas dépasser {max} caractères.")
    private String commentaire;

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
