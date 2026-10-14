package ar.edu.uade.api.pagos.excepcion;

// Unica excepcion de negocio del modulo: el TPO exige un codigo de error propio
// y un HTTP status por situacion, que ErrorCode lleva consigo.
public class PagoException extends RuntimeException {

    private final ErrorCode code;

    public PagoException(ErrorCode code, String mensaje) {
        super(mensaje);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
