package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO-02
@Schema(description = "Pago: estado actual, monto y medio de pago enmascarado")
public class PagoResponse {

    @Schema(description = "Codigo publico del pago (nunca se expone el id interno)")
    private final UUID codigoPago;

    private final UUID codigoPedido;

    @Schema(allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO", "REEMBOLSADO"}, example = "PENDIENTE")
    private final String estado;

    @Schema(example = "15000.00")
    private final BigDecimal monto;

    @Schema(example = "ARS")
    private final String moneda;

    private final MedioPagoResumenResponse medioPago;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    public PagoResponse(UUID codigoPago, UUID codigoPedido, String estado, BigDecimal monto, String moneda,
                        MedioPagoResumenResponse medioPago, LocalDateTime fechaCreacion,
                        LocalDateTime fechaActualizacion) {
        this.codigoPago = codigoPago;
        this.codigoPedido = codigoPedido;
        this.estado = estado;
        this.monto = monto;
        this.moneda = moneda;
        this.medioPago = medioPago;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public UUID getCodigoPago() { return codigoPago; }
    public UUID getCodigoPedido() { return codigoPedido; }
    public String getEstado() { return estado; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public MedioPagoResumenResponse getMedioPago() { return medioPago; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
