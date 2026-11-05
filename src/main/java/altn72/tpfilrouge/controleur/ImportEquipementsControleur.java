package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.exception.ImportInvalideException;
import altn72.tpfilrouge.modele.UtilisateurDetails;
import altn72.tpfilrouge.service.ImportEquipementsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/equipements/import")
public class ImportEquipementsControleur {

    private static final String VUE_IMPORT = "importEquipements";

    private final ImportEquipementsService importEquipementsService;

    public ImportEquipementsControleur(ImportEquipementsService importEquipementsService) {
        this.importEquipementsService = importEquipementsService;
    }

    @GetMapping
    public String afficherFormulaire() {
        return VUE_IMPORT;
    }

    @PostMapping
    public String importerEquipements(@RequestParam("fichier") MultipartFile fichier,
                                      RedirectAttributes attributsRedirection) throws IOException {
        int nombreImportes = importEquipementsService.importerEquipements(fichier.getBytes());
        attributsRedirection.addFlashAttribute("messageSucces", "Équipements importés : " + nombreImportes + ".");
        return "redirect:/equipements";
    }

    // Les erreurs restent sur le formulaire : l'utilisateur doit les voir toutes pour corriger son fichier.
    @ExceptionHandler(ImportInvalideException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String gererImportInvalide(ImportInvalideException exception,
                                      @AuthenticationPrincipal UtilisateurDetails utilisateurConnecte,
                                      Model model) {
        model.addAttribute("erreurs", exception.getErreurs());
        model.addAttribute("utilisateurConnecte", utilisateurConnecte);
        return VUE_IMPORT;
    }
}
