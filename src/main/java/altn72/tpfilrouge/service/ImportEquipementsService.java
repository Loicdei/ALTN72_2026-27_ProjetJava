package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.ImportInvalideException;
import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.repository.EquipementRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImportEquipementsService {

    private static final String COLONNE_REFERENCE = "reference";
    private static final String COLONNE_LIBELLE = "libelle";
    private static final String COLONNE_TYPE = "type";
    private static final String COLONNE_ETAT = "etat";
    private static final String COLONNE_STATION = "station";
    private static final List<String> COLONNES =
            List.of(COLONNE_REFERENCE, COLONNE_LIBELLE, COLONNE_TYPE, COLONNE_ETAT, COLONNE_STATION);
    // Mêmes longueurs que les colonnes de la table equipement : une valeur plus longue serait refusée par la base.
    private static final Map<String, Integer> LONGUEURS_MAX =
            Map.of(COLONNE_REFERENCE, 50, COLONNE_LIBELLE, 255, COLONNE_TYPE, 100);
    private static final Set<String> ETATS_AUTORISES = Set.of("EN_SERVICE", "EN_PANNE", "EN_MAINTENANCE");
    private static final int NUMERO_LIGNE_ENTETE = 1;

    private final EquipementRepository equipementRepository;
    private final StationService stationService;

    public ImportEquipementsService(EquipementRepository equipementRepository, StationService stationService) {
        this.equipementRepository = equipementRepository;
        this.stationService = stationService;
    }

    // Tout le fichier est importé, ou rien : un import à moitié fait laisserait des données incohérentes (RG10).
    @Transactional
    public int importerEquipements(byte[] fichier) {
        List<CSVRecord> lignes = lireLignes(decoder(fichier));
        List<Equipement> equipements = convertirLignes(lignes);
        try {
            equipementRepository.saveAll(equipements);
        } catch (DataIntegrityViolationException exception) {
            throw new ImportInvalideException("Une référence du fichier vient d'être créée par ailleurs. Aucun équipement n'a été importé.");
        }
        return equipements.size();
    }

    private String decoder(byte[] fichier) {
        try {
            String contenu = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(fichier)).toString();
            return contenu.startsWith(FormatCsv.MARQUE_UTF8) ? contenu.substring(FormatCsv.MARQUE_UTF8.length()) : contenu;
        } catch (CharacterCodingException exception) {
            throw new ImportInvalideException("Le fichier doit être encodé en UTF-8. Dans Excel, choisissez le format « CSV UTF-8 ».");
        }
    }

    private List<CSVRecord> lireLignes(String contenu) {
        if (contenu.isBlank()) {
            throw new ImportInvalideException("Le fichier est vide.");
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(FormatCsv.SEPARATEUR)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false)
                .get();
        try (CSVParser lecteur = CSVParser.parse(new StringReader(contenu), format)) {
            verifierColonnes(lecteur);
            List<CSVRecord> lignes = lecteur.stream().filter(this::contientDesDonnees).toList();
            if (lignes.isEmpty()) {
                throw new ImportInvalideException("Le fichier ne contient aucun équipement.");
            }
            return lignes;
        } catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
            throw new ImportInvalideException("Le fichier n'est pas un CSV valide. Vérifiez le séparateur « ; » et les guillemets.");
        }
    }

    private void verifierColonnes(CSVParser lecteur) {
        if (!lecteur.getHeaderMap().keySet().containsAll(COLONNES)) {
            throw new ImportInvalideException("La première ligne doit contenir les colonnes : " + String.join(";", COLONNES) + ".");
        }
    }

    private boolean contientDesDonnees(CSVRecord ligne) {
        return ligne.stream().anyMatch(valeur -> !valeur.isBlank());
    }

    private List<Equipement> convertirLignes(List<CSVRecord> lignes) {
        Map<String, Station> stations = stationService.obtenirToutesLesStations().stream()
                .collect(Collectors.toMap(Station::getNom, Function.identity(), (premiere, doublon) -> premiere));
        Set<String> referencesDuFichier = new HashSet<>();
        List<String> erreurs = new ArrayList<>();
        List<Equipement> equipements = new ArrayList<>();
        for (CSVRecord ligne : lignes) {
            List<String> erreursDeLaLigne = verifierLigne(ligne, stations, referencesDuFichier);
            if (erreursDeLaLigne.isEmpty()) {
                equipements.add(creerEquipement(ligne, stations));
            }
            erreurs.addAll(erreursDeLaLigne);
        }
        if (!erreurs.isEmpty()) {
            throw new ImportInvalideException(erreurs);
        }
        return equipements;
    }

    private List<String> verifierLigne(CSVRecord ligne, Map<String, Station> stations, Set<String> referencesDuFichier) {
        String prefixe = "Ligne " + (ligne.getRecordNumber() + NUMERO_LIGNE_ENTETE) + " : ";
        List<String> erreurs = new ArrayList<>();
        for (String colonne : COLONNES) {
            if (valeur(ligne, colonne).isEmpty()) {
                erreurs.add(prefixe + "la colonne « " + colonne + " » est obligatoire.");
            }
        }
        LONGUEURS_MAX.forEach((colonne, longueurMax) -> {
            if (valeur(ligne, colonne).length() > longueurMax) {
                erreurs.add(prefixe + "la colonne « " + colonne + " » dépasse " + longueurMax + " caractères.");
            }
        });
        verifierReference(valeur(ligne, COLONNE_REFERENCE), referencesDuFichier, prefixe, erreurs);
        verifierEtat(valeur(ligne, COLONNE_ETAT), prefixe, erreurs);
        verifierStation(valeur(ligne, COLONNE_STATION), stations, prefixe, erreurs);
        return erreurs;
    }

    private void verifierReference(String reference, Set<String> referencesDuFichier, String prefixe, List<String> erreurs) {
        if (reference.isEmpty()) {
            return;
        }
        if (!referencesDuFichier.add(reference)) {
            erreurs.add(prefixe + "la référence « " + reference + " » apparaît plusieurs fois dans le fichier.");
        } else if (equipementRepository.existsByReference(reference)) {
            erreurs.add(prefixe + "la référence « " + reference + " » existe déjà.");
        }
    }

    private void verifierEtat(String etat, String prefixe, List<String> erreurs) {
        if (!etat.isEmpty() && !ETATS_AUTORISES.contains(etat)) {
            erreurs.add(prefixe + "l'état « " + etat + " » est inconnu. Valeurs possibles : EN_SERVICE, EN_PANNE, EN_MAINTENANCE.");
        }
    }

    private void verifierStation(String station, Map<String, Station> stations, String prefixe, List<String> erreurs) {
        if (!station.isEmpty() && !stations.containsKey(station)) {
            erreurs.add(prefixe + "la station « " + station + " » n'existe pas.");
        }
    }

    private Equipement creerEquipement(CSVRecord ligne, Map<String, Station> stations) {
        return new Equipement(
                valeur(ligne, COLONNE_REFERENCE),
                valeur(ligne, COLONNE_LIBELLE),
                valeur(ligne, COLONNE_TYPE),
                valeur(ligne, COLONNE_ETAT),
                stations.get(valeur(ligne, COLONNE_STATION)));
    }

    private String valeur(CSVRecord ligne, String colonne) {
        return ligne.isSet(colonne) ? ligne.get(colonne).strip() : "";
    }
}
