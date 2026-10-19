package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// DTO-04
@Schema(description = "Solicitud de reembolso total o parcial")
public class ReembolsoRequest {

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Monto a devolver; no puede superar lo disponible (monto aprobado menos lo ya reembolsado)",
            example = "5000.00")
    private BigDecimal monto;

    @NotBlank
    @Size(max = 255)
    @Schema(example = "Pedido cancelado por el cliente")
    private String motivo;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Clave de control del reembolso (RN-10): repetirla devuelve el reembolso existente",
            example = "reembolso-0001-1")
    private String claveControl;

    public ReembolsoRequest() {
    }

    public ReembolsoRequest(BigDecimal monto, String motivo, String claveControl) {
        this.monto = monto;
        this.motivo = motivo;
        this.claveControl = claveControl;
    }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getClaveControl() { return claveControl; }
    public void setClaveControl(String claveControl) { this.claveControl = claveControl; }
}
