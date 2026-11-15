package ar.edu.uade.api.pagos.excepcion;

import ar.edu.uade.api.pagos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
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

import java.util.stream.Collectors;

// Traduce cada excepcion al error normalizado (DTO-10). Nunca expone detalles internos ni stack traces.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Content-Type fijo en JSON: el error se puede escribir aunque el cliente haya pedido otro formato (406).
    private ResponseEntity<ErrorResponse> build(ErrorCode code, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(code.getStatus()).contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseFactory.crear(code, mensaje, request));
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

    // Choque con el UNIQUE de clave_control (carrera no resuelta por el Service) -> 409.
    // Cualquier otra violacion de integridad es un error nuestro, no del cliente -> 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest r) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (causa.toLowerCase().contains("clave_control")) {
            log.warn("Clave de control repetida: {}", causa);
            return build(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                    "La solicitud entra en conflicto con un registro existente (clave de control repetida)", r);
        }
        log.error("Violacion de integridad inesperada en {}: {}", r.getRequestURI(), causa);
        return build(ErrorCode.INTERNAL_ERROR, "Error interno del servidor", r);
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

    // El cuerpo no vino como application/json.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> contentType(HttpMediaTypeNotSupportedException ex, HttpServletRequest r) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type no soportado: " + ex.getContentType() + ". Usar application/json", r);
    }

    // El cliente pide (header Accept) un formato que la API no produce.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> accept(HttpMediaTypeNotAcceptableException ex, HttpServletRequest r) {
        return build(ErrorCode.NOT_ACCEPTABLE, "La API solo responde application/json", r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest r) {
        log.error("Error inesperado en {}", r.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Error interno del servidor", r);
    }
}
