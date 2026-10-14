package ar.edu.uade.api.pagos.dto;

// DTO-03: motivo (opcional) del rechazo.
public class RechazarPagoRequestDto {

    private String motivo;

    public RechazarPagoRequestDto() {
    }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
