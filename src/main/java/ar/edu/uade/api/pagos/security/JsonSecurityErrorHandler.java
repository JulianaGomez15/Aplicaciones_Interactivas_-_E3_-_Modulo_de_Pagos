package ar.edu.uade.api.pagos.security;

import ar.edu.uade.api.pagos.config.CorrelationIdFilter;
import ar.edu.uade.api.pagos.dto.ErrorResponseDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

// 401 / 403 con el mismo formato de error que el resto de la API.
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribir(request, response, ErrorCode.UNAUTHORIZED, "Falta autenticacion o el token es invalido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(request, response, ErrorCode.FORBIDDEN, "El usuario no tiene permiso para esta operacion");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String mensaje)
            throws IOException {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        ErrorResponseDto body = new ErrorResponseDto(OffsetDateTime.now(), code.getStatus().value(), code.name(),
                mensaje, request.getRequestURI(), correlationId == null ? null : correlationId.toString());

        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
