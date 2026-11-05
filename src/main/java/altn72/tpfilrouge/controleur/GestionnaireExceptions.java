package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.exception.DonneeInvalideException;
import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.UtilisateurDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GestionnaireExceptions {

    private static final String VUE_ERREUR = "error";

    @ExceptionHandler(RessourceIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String gererRessourceIntrouvable(RessourceIntrouvableException exception,
                                            @AuthenticationPrincipal UtilisateurDetails utilisateurConnecte,
                                            Model model) {
        return afficherErreur(exception.getMessage(), utilisateurConnecte, model);
    }

    @ExceptionHandler(OperationIncidentInterditeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String gererOperationInterdite(OperationIncidentInterditeException exception,
                                          @AuthenticationPrincipal UtilisateurDetails utilisateurConnecte,
                                          Model model) {
        return afficherErreur(exception.getMessage(), utilisateurConnecte, model);
    }

    @ExceptionHandler(DonneeInvalideException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String gererDonneeInvalide(DonneeInvalideException exception,
                                      @AuthenticationPrincipal UtilisateurDetails utilisateurConnecte,
                                      Model model) {
        return afficherErreur(exception.getMessage(), utilisateurConnecte, model);
    }

    private String afficherErreur(String message, UtilisateurDetails utilisateurConnecte, Model model) {
        model.addAttribute("messageErreur", message);
        // Spring n'applique pas les @ModelAttribute globaux aux gestionnaires d'exceptions : l'en-tête en a pourtant besoin.
        model.addAttribute("utilisateurConnecte", utilisateurConnecte);
        return VUE_ERREUR;
    }
}
