package ar.edu.uade.api.pagos.excepcion;

import ar.edu.uade.api.pagos.config.CorrelationIdFilter;
import ar.edu.uade.api.pagos.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponseDto> build(ErrorCode code, String mensaje, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        ErrorResponseDto body = new ErrorResponseDto(OffsetDateTime.now(), code.getStatus().value(), code.name(),
                mensaje, request.getRequestURI(), correlationId == null ? null : correlationId.toString());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(PagoException.class)
    public ResponseEntity<ErrorResponseDto> negocio(PagoException ex, HttpServletRequest r) {
        return build(ex.getCode(), ex.getMessage(), r);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, PropertyReferenceException.class})
    public ResponseEntity<ErrorResponseDto> malFormado(Exception ex, HttpServletRequest r) {
        return build(ErrorCode.BAD_REQUEST, "Solicitud mal formada: algun parametro o el cuerpo no es valido", r);
    }

    // Dos solicitudes simultaneas con la misma clave de control: gana una, la otra choca con el UNIQUE.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> integridad(DataIntegrityViolationException ex, HttpServletRequest r) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                "La solicitud entra en conflicto con un pago existente (clave de control repetida)", r);
    }

    // Dos operaciones simultaneas sobre el mismo pago.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> concurrencia(ObjectOptimisticLockingFailureException ex,
                                                         HttpServletRequest r) {
        return build(ErrorCode.PAYMENT_INVALID_STATE,
                "El pago fue modificado por otra operacion al mismo tiempo; reintentar", r);
    }

    // Login con credenciales incorrectas (AuthController).
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> credenciales(AuthenticationException ex, HttpServletRequest r) {
        return build(ErrorCode.UNAUTHORIZED, "Usuario o contrasena incorrectos", r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> sinPermiso(AccessDeniedException ex, HttpServletRequest r) {
        return build(ErrorCode.FORBIDDEN, "El usuario no tiene permiso para esta operacion", r);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> noEncontrado(NoResourceFoundException ex, HttpServletRequest r) {
        return build(ErrorCode.NOT_FOUND, "Recurso inexistente: " + r.getRequestURI(), r);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest r) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> inesperado(Exception ex, HttpServletRequest r) {
        log.error("Error inesperado en {}", r.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Error interno del servidor", r);
    }
}
