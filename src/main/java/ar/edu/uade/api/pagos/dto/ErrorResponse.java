package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// DTO-10: formato normalizado de error del ecosistema.
@Schema(description = "Error normalizado")
public class ErrorResponse {

    @Schema(description = "Fecha y hora del error (ISO-8601)")
    private final OffsetDateTime timestamp;

    @Schema(example = "409")
    private final int status;

    @Schema(description = "Codigo de error propio del modulo", example = "PAYMENT_INVALID_STATE")
    private final String code;

    @Schema(description = "Mensaje descriptivo para quien consume la API")
    private final String message;

    @Schema(example = "/api/v1/payments/6d3a0b2e-0000-0000-0000-000000000000/approve")
    private final String path;

    @Schema(description = "Tomado del header X-Correlation-Id (o generado si no vino)")
    private final String correlationId;

    public ErrorResponse(OffsetDateTime timestamp, int status, String code, String message, String path,
                         String correlationId) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
    }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getCorrelationId() { return correlationId; }
}
