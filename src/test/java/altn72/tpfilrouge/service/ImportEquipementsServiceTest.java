package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.ImportInvalideException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.repository.EquipementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportEquipementsServiceTest {

    private static final String ENTETE = "reference;libelle;type;etat;station\n";
    private static final String LIGNE_VALIDE = "EQ-1;Portique;Portique;EN_SERVICE;Station Nord\n";

    @Mock
    private EquipementRepository equipementRepository;

    @Mock
    private StationService stationService;

    @Captor
    private ArgumentCaptor<List<Equipement>> equipementsEnregistres;

    private ImportEquipementsService importService;
    private Station stationNord;

    @BeforeEach
    void creerService() {
        stationNord = new Station("Station Nord");
        lenient().when(stationService.obtenirToutesLesStations()).thenReturn(List.of(stationNord));
        importService = new ImportEquipementsService(equipementRepository, stationService);
    }

    private int importer(String contenu) {
        return importService.importerEquipements(contenu.getBytes(StandardCharsets.UTF_8));
    }

    private void verifierRefus(String contenu, String... messages) {
        assertThatThrownBy(() -> importer(contenu))
                .isInstanceOfSatisfying(ImportInvalideException.class,
                        exception -> assertThat(exception.getErreurs()).containsExactly(messages));
        verify(equipementRepository, never()).saveAll(any());
    }

    @Test
    void unFichierValideCreeLesEquipementsDansLeursStations() {
        int nombre = importer(ENTETE + LIGNE_VALIDE + "EQ-2;Écran hall;Écran;EN_PANNE;Station Nord\n");

        assertThat(nombre).isEqualTo(2);
        verify(equipementRepository).saveAll(equipementsEnregistres.capture());
        Equipement ecran = equipementsEnregistres.getValue().get(1);
        assertThat(ecran.getReference()).isEqualTo("EQ-2");
        assertThat(ecran.getLibelle()).isEqualTo("Écran hall");
        assertThat(ecran.getType()).isEqualTo("Écran");
        assertThat(ecran.getEtatFonctionnement()).isEqualTo("EN_PANNE");
        assertThat(ecran.getStation()).isSameAs(stationNord);
    }

    @Test
    void laMarqueUtf8DExcelEtLesLignesVidesSontToleres() {
        assertThat(importer(FormatCsv.MARQUE_UTF8 + ENTETE + "\n" + LIGNE_VALIDE + "\n")).isEqualTo(1);
    }

    @Test
    void uneLigneInvalideEmpecheToutLImport() {
        verifierRefus(ENTETE + LIGNE_VALIDE + "EQ-2;Écran;Écran;EN_SERVICE;Station Inconnue\n",
                "Ligne 3 : la station « Station Inconnue » n'existe pas.");
    }

    @Test
    void toutesLesErreursSontSignaleesAvecLeurLigne() {
        verifierRefus(ENTETE + "EQ-1;;Portique;CASSE;Station Nord\nEQ-2;Écran;Écran;EN_SERVICE;\n",
                "Ligne 2 : la colonne « libelle » est obligatoire.",
                "Ligne 2 : l'état « CASSE » est inconnu. Valeurs possibles : EN_SERVICE, EN_PANNE, EN_MAINTENANCE.",
                "Ligne 3 : la colonne « station » est obligatoire.");
    }

    @Test
    void uneLigneIncompleteEstSignaleeSansErreurTechnique() {
        verifierRefus(ENTETE + "EQ-1;Portique;Portique;EN_SERVICE\n",
                "Ligne 2 : la colonne « station » est obligatoire.");
    }

    @Test
    void uneReferenceEnDoubleDansLeFichierEstRefusee() {
        verifierRefus(ENTETE + LIGNE_VALIDE + LIGNE_VALIDE,
                "Ligne 3 : la référence « EQ-1 » apparaît plusieurs fois dans le fichier.");
    }

    @Test
    void uneReferenceDejaExistanteEstRefusee() {
        when(equipementRepository.existsByReference("EQ-1")).thenReturn(true);

        verifierRefus(ENTETE + LIGNE_VALIDE, "Ligne 2 : la référence « EQ-1 » existe déjà.");
    }

    @Test
    void uneValeurTropLongueEstRefusee() {
        String reference = "R".repeat(51);

        verifierRefus(ENTETE + reference + ";Portique;Portique;EN_SERVICE;Station Nord\n",
                "Ligne 2 : la colonne « reference » dépasse 50 caractères.");
    }

    @Test
    void unFichierVideEstRefuse() {
        verifierRefus("  ", "Le fichier est vide.");
    }

    @Test
    void unFichierSansEquipementEstRefuse() {
        verifierRefus(ENTETE, "Le fichier ne contient aucun équipement.");
    }

    @Test
    void desColonnesManquantesSontRefusees() {
        verifierRefus("reference,libelle,type,etat,station\nEQ-1,a,b,c,d\n",
                "La première ligne doit contenir les colonnes : reference;libelle;type;etat;station.");
    }

    @Test
    void desGuillemetsMalFormesSontRefusesSansErreurTechnique() {
        verifierRefus(ENTETE + "\"EQ-1\"x;Portique;Portique;EN_SERVICE;Station Nord\n",
                "Le fichier n'est pas un CSV valide. Vérifiez le séparateur « ; » et les guillemets.");
    }

    @Test
    void unFichierQuiNEstPasEnUtf8EstRefuse() {
        byte[] contenu = (ENTETE + "EQ-1;Écran;Écran;EN_SERVICE;Station Nord\n").getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> importService.importerEquipements(contenu))
                .isInstanceOf(ImportInvalideException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    void uneReferenceCreeeEnMemeTempsEstSignalee() {
        when(equipementRepository.saveAll(any())).thenThrow(new DataIntegrityViolationException("doublon"));

        assertThatThrownBy(() -> importer(ENTETE + LIGNE_VALIDE))
                .isInstanceOf(ImportInvalideException.class)
                .hasMessageContaining("Aucun équipement n'a été importé");
    }
}
