package cl.duoc.dsy2207.bff.exception;

public class FuncionNoDisponibleException extends RuntimeException {
    public FuncionNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public FuncionNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
