package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.formulaire.FormulaireAffectation;
import altn72.tpfilrouge.formulaire.FormulaireIncident;
import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.service.EquipementService;
import altn72.tpfilrouge.service.IncidentService;
import altn72.tpfilrouge.service.PrestataireService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IncidentControleur {

    private static final String VUE_DECLARATION = "nouvelIncident";
    private static final String VUE_DETAIL = "detailIncident";
    private static final String VUE_MODIFICATION = "modifierIncident";
    private static final String VUE_HISTORIQUE = "historiqueIncidents";

    private final IncidentService incidentService;
    private final EquipementService equipementService;
    private final PrestataireService prestataireService;

    public IncidentControleur(IncidentService incidentService,
                              EquipementService equipementService,
                              PrestataireService prestataireService) {
        this.incidentService = incidentService;
        this.equipementService = equipementService;
        this.prestataireService = prestataireService;
    }

    @GetMapping("/equipements/{equipementId}/incidents/nouveau")
    public String afficherFormulaireDeclaration(@PathVariable Long equipementId, Model model) {
        model.addAttribute("equipement", equipementService.obtenirParId(equipementId));
        model.addAttribute("formulaireIncident", new FormulaireIncident());
        return VUE_DECLARATION;
    }

    @PostMapping("/equipements/{equipementId}/incidents")
    public String declarerIncident(@PathVariable Long equipementId,
                                   @Valid @ModelAttribute("formulaireIncident") FormulaireIncident formulaire,
                                   BindingResult resultatValidation,
                                   Model model,
                                   RedirectAttributes attributsRedirection) {
        if (resultatValidation.hasErrors()) {
            model.addAttribute("equipement", equipementService.obtenirParId(equipementId));
            return VUE_DECLARATION;
        }
        Incident incident = incidentService.declarerIncident(equipementId, formulaire.getDescription());
        attributsRedirection.addFlashAttribute("messageSucces", "L'incident a bien été déclaré.");
        return "redirect:/incidents/" + incident.getId();
    }

    @GetMapping("/incidents/historique")
    public String afficherHistorique(Model model) {
        model.addAttribute("incidents", incidentService.obtenirHistoriqueDesIncidentsResolus());
        return VUE_HISTORIQUE;
    }

    @GetMapping("/incidents/{id}")
    public String afficherDetail(@PathVariable Long id, Model model) {
        Incident incident = incidentService.obtenirParId(id);
        FormulaireAffectation formulaire = new FormulaireAffectation();
        if (incident.getPrestataire() != null) {
            formulaire.setPrestataireId(incident.getPrestataire().getId());
        }
        model.addAttribute("formulaireAffectation", formulaire);
        return preparerVueDetail(incident, model);
    }

    @PostMapping("/incidents/{id}/affectation")
    public String affecterPrestataire(@PathVariable Long id,
                                      @Valid @ModelAttribute("formulaireAffectation") FormulaireAffectation formulaire,
                                      BindingResult resultatValidation,
                                      Model model,
                                      RedirectAttributes attributsRedirection) {
        if (resultatValidation.hasErrors()) {
            return preparerVueDetail(incidentService.obtenirParId(id), model);
        }
        incidentService.affecterPrestataire(id, formulaire.getPrestataireId());
        attributsRedirection.addFlashAttribute("messageSucces", "Le prestataire a bien été affecté.");
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/resolution")
    public String declarerResolu(@PathVariable Long id, RedirectAttributes attributsRedirection) {
        incidentService.declarerResolu(id);
        attributsRedirection.addFlashAttribute("messageSucces", "L'incident a bien été déclaré résolu.");
        return "redirect:/incidents/" + id;
    }

    private String preparerVueDetail(Incident incident, Model model) {
        model.addAttribute("incident", incident);
        model.addAttribute("dureeOuverture", incidentService.calculerDureeOuverture(incident));
        model.addAttribute("evaluation", incidentService.obtenirEvaluation(incident.getId()).orElse(null));
        model.addAttribute("prestataires", prestataireService.obtenirTousLesPrestataires());
        return VUE_DETAIL;
    }

    @GetMapping("/incidents/{id}/modifier")
    public String afficherFormulaireModification(@PathVariable Long id, Model model) {
        Incident incident = incidentService.obtenirIncidentModifiable(id);
        FormulaireIncident formulaire = new FormulaireIncident();
        formulaire.setDescription(incident.getDescription());
        model.addAttribute("incident", incident);
        model.addAttribute("formulaireIncident", formulaire);
        return VUE_MODIFICATION;
    }

    @PostMapping("/incidents/{id}/modifier")
    public String modifierIncident(@PathVariable Long id,
                                   @Valid @ModelAttribute("formulaireIncident") FormulaireIncident formulaire,
                                   BindingResult resultatValidation,
                                   Model model,
                                   RedirectAttributes attributsRedirection) {
        if (resultatValidation.hasErrors()) {
            model.addAttribute("incident", incidentService.obtenirIncidentModifiable(id));
            return VUE_MODIFICATION;
        }
        incidentService.modifierDescription(id, formulaire.getDescription());
        attributsRedirection.addFlashAttribute("messageSucces", "L'incident a bien été modifié.");
        return "redirect:/incidents/" + id;
    }
}
