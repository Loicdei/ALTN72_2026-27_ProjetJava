package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.service.EquipementService;
import altn72.tpfilrouge.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/equipements")
public class EquipementControleur {

    private final EquipementService equipementService;
    private final StationService stationService;

    public EquipementControleur(EquipementService equipementService, StationService stationService) {
        this.equipementService = equipementService;
        this.stationService = stationService;
    }

    @GetMapping
    public String afficherEquipementsParStation(@RequestParam(name = "stationId", required = false) Long stationId, Model model) {
        model.addAttribute("stations", stationService.obtenirToutesLesStations());
        model.addAttribute("selectedStationId", stationId);

        if (stationId != null) {
            // Filtrer par station si une station est sélectionnée
            model.addAttribute("equipements", equipementService.obtenirEquipementsParStation(stationId));
        } else {
            // Afficher TOUS les équipements au premier chargement
            model.addAttribute("equipements", equipementService.obtenirTousLesEquipements());
        }

        return "equipements";
    }
}