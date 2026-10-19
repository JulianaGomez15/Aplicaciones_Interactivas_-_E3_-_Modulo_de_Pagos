package ar.edu.uade.api.pagos.excepcion;

import ar.edu.uade.api.pagos.config.CorrelationIdFilter;
import ar.edu.uade.api.pagos.dto.ErrorResponse;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

// Traduce cada excepcion al error normalizado (DTO-10). Nunca expone detalles internos ni stack traces.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String mensaje, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        ErrorResponse body = new ErrorResponse(OffsetDateTime.now(), code.getStatus().value(), code.name(),
                mensaje, request.getRequestURI(), correlationId == null ? null : correlationId.toString());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(PagoException.class)
    public ResponseEntity<ErrorResponse> negocio(PagoException ex, HttpServletRequest r) {
        return build(ex.getCode(), ex.getMessage(), r);
    }

    // @Valid en el controlador: faltan campos obligatorios o tienen un formato invalido.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest r) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(ErrorCode.VALIDATION_ERROR, "Solicitud invalida - " + detalle, r);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, PropertyReferenceException.class})
    public ResponseEntity<ErrorResponse> malFormado(Exception ex, HttpServletRequest r) {
        return build(ErrorCode.VALIDATION_ERROR,
                "Solicitud mal formada: algun parametro o el cuerpo no tiene el formato esperado", r);
    }

    // Choque con un UNIQUE (clave de control repetida en una carrera no resuelta por el Service).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest r) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                "La solicitud entra en conflicto con un registro existente (clave de control repetida)", r);
    }

    // Dos operaciones simultaneas sobre el mismo pago (@Version).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> concurrencia(ObjectOptimisticLockingFailureException ex,
                                                      HttpServletRequest r) {
        return build(ErrorCode.PAYMENT_INVALID_STATE,
                "El pago fue modificado por otra operacion al mismo tiempo; reintentar", r);
    }

    // Login con credenciales incorrectas (AuthController).
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> credenciales(AuthenticationException ex, HttpServletRequest r) {
        return build(ErrorCode.UNAUTHORIZED, "Usuario o contrasena incorrectos", r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> sinPermiso(AccessDeniedException ex, HttpServletRequest r) {
        return build(ErrorCode.FORBIDDEN, "El rol del usuario no tiene permiso para esta operacion", r);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoResourceFoundException ex, HttpServletRequest r) {
        return build(ErrorCode.NOT_FOUND, "Recurso inexistente: " + r.getRequestURI(), r);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest r) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest r) {
        log.error("Error inesperado en {}", r.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Error interno del servidor", r);
    }
}
