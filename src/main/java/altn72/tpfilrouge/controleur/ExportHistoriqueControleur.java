package altn72.tpfilrouge.controleur;

import altn72.tpfilrouge.service.ExportHistoriqueService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;

@Controller
public class ExportHistoriqueControleur {

    private static final MediaType TYPE_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final String NOM_FICHIER = "historique-incidents.csv";

    private final ExportHistoriqueService exportHistoriqueService;

    public ExportHistoriqueControleur(ExportHistoriqueService exportHistoriqueService) {
        this.exportHistoriqueService = exportHistoriqueService;
    }

    // ResponseEntity est utile ici : le type de contenu et le nom du fichier téléchargé passent par les en-têtes.
    @GetMapping("/incidents/historique/export")
    public ResponseEntity<String> exporterHistorique() {
        return ResponseEntity.ok()
                .contentType(TYPE_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(NOM_FICHIER).build().toString())
                .body(exportHistoriqueService.exporterHistorique());
    }
}
