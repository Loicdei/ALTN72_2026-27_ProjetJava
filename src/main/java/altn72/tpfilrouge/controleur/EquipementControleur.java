package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.service.EquipementFragiliteService;
import altn72.tpfilrouge.service.EquipementService;
import altn72.tpfilrouge.service.IncidentService;
import altn72.tpfilrouge.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/equipements")
public class EquipementControleur {

    private final EquipementService equipementService;
    private final StationService stationService;
    private final IncidentService incidentService;
    private final EquipementFragiliteService equipementFragiliteService;

    public EquipementControleur(EquipementService equipementService, StationService stationService,
                                IncidentService incidentService, EquipementFragiliteService equipementFragiliteService) {
        this.equipementService = equipementService;
        this.stationService = stationService;
        this.incidentService = incidentService;
        this.equipementFragiliteService = equipementFragiliteService;
    }

    @GetMapping
    public String afficherEquipementsParStation(@RequestParam(name = "stationId", required = false) Long stationId, Model model) {
        model.addAttribute("stations", stationService.obtenirToutesLesStations());
        model.addAttribute("selectedStationId", stationId);

        if (stationId != null) {
            model.addAttribute("equipements", equipementService.obtenirEquipementsParStation(stationId));
        } else {
            model.addAttribute("equipements", equipementService.obtenirTousLesEquipements());
        }

        return "equipements";
    }

    @GetMapping("/{id}")
    public String afficherFicheEquipement(@PathVariable("id") Long id, Model model) {
        Equipement equipement = equipementService.obtenirParId(id);
        List<Incident> incidents = incidentService.obtenirIncidentsParEquipement(id);

        model.addAttribute("equipement", equipement);
        model.addAttribute("incidents", incidents);
        model.addAttribute("equipementFragile", equipementFragiliteService.estFragile(id));

        return "detailEquipement";
    }
}