package ar.edu.uade.api.pagos.dto;

import java.time.OffsetDateTime;

public class ErrorResponseDto {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final String correlationId;

    public ErrorResponseDto(OffsetDateTime timestamp, int status, String code, String message, String path,
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
