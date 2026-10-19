package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// DTO-09
@Schema(description = "Un cambio de estado del pago")
public class HistorialEstadoResponse {

    @Schema(description = "Null en el alta del pago", nullable = true, example = "PENDIENTE")
    private final String estadoAnterior;

    @Schema(example = "APROBADO")
    private final String estadoNuevo;

    private final String motivo;
    private final LocalDateTime fecha;

    public HistorialEstadoResponse(String estadoAnterior, String estadoNuevo, String motivo, LocalDateTime fecha) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }
}
