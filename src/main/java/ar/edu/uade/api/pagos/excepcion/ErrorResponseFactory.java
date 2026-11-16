package ar.edu.uade.api.pagos.excepcion;

import ar.edu.uade.api.pagos.config.CorrelationIdFilter;
import ar.edu.uade.api.pagos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

// Unico lugar donde se arma el error normalizado (DTO-10): lo usan el GlobalExceptionHandler
// y el manejador de errores de seguridad (401 / 403).
public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ErrorResponse crear(ErrorCode code, String mensaje, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return new ErrorResponse(LocalDateTime.now(), code.getStatus().value(), code.name(), mensaje,
                request.getRequestURI(), correlationId == null ? null : correlationId.toString());
    }
}
