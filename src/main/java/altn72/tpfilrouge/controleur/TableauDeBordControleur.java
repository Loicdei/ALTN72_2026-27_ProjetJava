package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.formulaire.FiltreTableauDeBord;
import altn72.tpfilrouge.modele.NiveauAttention;
import altn72.tpfilrouge.service.EquipementService;
import altn72.tpfilrouge.service.StationService;
import altn72.tpfilrouge.service.TableauDeBordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class TableauDeBordControleur {

    private final TableauDeBordService tableauDeBordService;
    private final StationService stationService;
    private final EquipementService equipementService;

    public TableauDeBordControleur(TableauDeBordService tableauDeBordService,
                                   StationService stationService,
                                   EquipementService equipementService) {
        this.tableauDeBordService = tableauDeBordService;
        this.stationService = stationService;
        this.equipementService = equipementService;
    }

    @GetMapping("/tableau-de-bord")
    public String afficherTableauDeBord(@ModelAttribute("filtre") FiltreTableauDeBord filtre, Model model) {
        model.addAttribute("tableau", tableauDeBordService.construireTableauDeBord(filtre));
        model.addAttribute("stations", stationService.obtenirToutesLesStations());
        model.addAttribute("types", equipementService.obtenirTypes());
        model.addAttribute("niveaux", NiveauAttention.values());
        return "tableauDeBord";
    }
}
