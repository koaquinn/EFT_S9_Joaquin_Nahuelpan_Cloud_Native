package cl.duoc.dsy2207.bff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalErrores {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Recurso no encontrado");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Solicitud inválida");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(FuncionNoDisponibleException.class)
    public ProblemDetail manejarFuncionNoDisponible(FuncionNoDisponibleException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Función serverless no disponible");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarGenerico(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Error interno del BFF");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
