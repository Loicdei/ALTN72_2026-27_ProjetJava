package altn72.tpfilrouge.api;

import altn72.tpfilrouge.exception.OperationIncidentInterditeException;
import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Passe avant GestionnaireExceptions : un client de l'API attend du JSON, pas la page d'erreur HTML.
@RestControllerAdvice(basePackageClasses = IncidentApiControleur.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionnaireExceptionsApi {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail gererRessourceIntrouvable(RessourceIntrouvableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OperationIncidentInterditeException.class)
    public ProblemDetail gererOperationInterdite(OperationIncidentInterditeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail gererCorpsInvalide(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail gererParametreMalForme(MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La valeur du paramètre « " + exception.getName() + " » n'est pas valide.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail gererCorpsIllisible(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Le corps de la requête n'est pas un JSON valide.");
    }
}
