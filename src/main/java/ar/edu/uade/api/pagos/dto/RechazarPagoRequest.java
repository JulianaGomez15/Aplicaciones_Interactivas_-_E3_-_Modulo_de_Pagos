package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// DTO-03
@Schema(description = "Motivo del rechazo simulado")
public class RechazarPagoRequest {

    @Size(max = 255)
    @Schema(description = "Motivo del rechazo (opcional; por defecto 'Rechazo simulado')", example = "Fondos insuficientes")
    private String motivo;

    public RechazarPagoRequest() {
    }

    public RechazarPagoRequest(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
