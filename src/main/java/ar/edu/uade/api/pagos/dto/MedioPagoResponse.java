package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

// DTO-08
@Schema(description = "Medio de pago registrado (solo datos enmascarados)")
public class MedioPagoResponse {

    private final UUID codigoMedioPago;

    @Schema(example = "TARJETA_CREDITO")
    private final String tipo;

    @Schema(example = "**** **** **** 1234")
    private final String detalleEnmascarado;

    private final boolean activo;

    public MedioPagoResponse(UUID codigoMedioPago, String tipo, String detalleEnmascarado, boolean activo) {
        this.codigoMedioPago = codigoMedioPago;
        this.tipo = tipo;
        this.detalleEnmascarado = detalleEnmascarado;
        this.activo = activo;
    }

    public UUID getCodigoMedioPago() { return codigoMedioPago; }
    public String getTipo() { return tipo; }
    public String getDetalleEnmascarado() { return detalleEnmascarado; }
    public boolean isActivo() { return activo; }
}
