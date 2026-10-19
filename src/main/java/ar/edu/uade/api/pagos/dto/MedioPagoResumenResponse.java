package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Medio de pago tal como se ve dentro de DTO-02: solo tipo y detalle enmascarado.
@Schema(description = "Medio de pago utilizado (solo datos enmascarados)")
public class MedioPagoResumenResponse {

    @Schema(example = "TARJETA_CREDITO")
    private final String tipo;

    @Schema(example = "**** **** **** 1234")
    private final String detalleEnmascarado;

    public MedioPagoResumenResponse(String tipo, String detalleEnmascarado) {
        this.tipo = tipo;
        this.detalleEnmascarado = detalleEnmascarado;
    }

    public String getTipo() { return tipo; }
    public String getDetalleEnmascarado() { return detalleEnmascarado; }
}
