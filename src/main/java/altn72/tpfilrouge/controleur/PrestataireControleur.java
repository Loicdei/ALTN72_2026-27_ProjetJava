package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.service.PrestataireService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrestataireControleur {

    private final PrestataireService prestataireService;

    public PrestataireControleur(PrestataireService prestataireService) {
        this.prestataireService = prestataireService;
    }

    @GetMapping("/prestataires")
    public String afficherPrestataires(Model model) {
        model.addAttribute("notesPrestataires", prestataireService.obtenirNotesDesPrestataires());
        return "listePrestataires";
    }
}
