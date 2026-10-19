package ar.edu.uade.api.pagos.excepcion;

import org.springframework.http.HttpStatus;

// Codigos de error del modulo (seccion "Manejo de errores" del TPO) + dos tecnicos (rutas inexistentes / metodo).
public enum ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_INVALID_STATE(HttpStatus.CONFLICT),
    DUPLICATE_PAYMENT_REQUEST(HttpStatus.CONFLICT),
    REFUND_EXCEEDS_AMOUNT(HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_METHOD_INVALID(HttpStatus.BAD_REQUEST),
    PAYMENT_USER_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),
    ORDER_NOT_PAYABLE(HttpStatus.UNPROCESSABLE_ENTITY),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // Tecnicos, fuera del listado del TPO.
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
