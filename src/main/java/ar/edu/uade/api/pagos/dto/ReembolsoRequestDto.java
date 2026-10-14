package ar.edu.uade.api.pagos.dto;

import java.math.BigDecimal;

// DTO-04: solicitud de reembolso.
public class ReembolsoRequestDto {

    private BigDecimal monto;
    private String motivo;

    public ReembolsoRequestDto() {
    }

    public ReembolsoRequestDto(BigDecimal monto, String motivo) {
        this.monto = monto;
        this.motivo = motivo;
    }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
