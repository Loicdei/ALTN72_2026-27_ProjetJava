package altn72.tpfilrouge.service;

final class FormatCsv {

    // Le point-virgule est le séparateur attendu par Excel en français.
    static final char SEPARATEUR = ';';

    // Sans cette marque en tête de fichier, Excel lit l'UTF-8 comme de l'ANSI et abîme les accents.
    static final String MARQUE_UTF8 = "﻿";

    private FormatCsv() {
    }
}
