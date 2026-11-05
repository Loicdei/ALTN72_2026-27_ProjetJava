package altn72.tpfilrouge.exception;

import java.util.List;

public class ImportInvalideException extends RuntimeException {

    private final List<String> erreurs;

    public ImportInvalideException(List<String> erreurs) {
        super(String.join(" ", erreurs));
        this.erreurs = List.copyOf(erreurs);
    }

    public ImportInvalideException(String erreur) {
        this(List.of(erreur));
    }

    public List<String> getErreurs() {
        return erreurs;
    }
}
