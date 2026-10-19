package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO-05
@Schema(description = "Reembolso registrado (simulado)")
public class ReembolsoResponse {

    private final UUID codigoReembolso;
    private final UUID codigoPago;

    @Schema(example = "5000.00")
    private final BigDecimal monto;

    @Schema(allowableValues = {"APROBADO"}, example = "APROBADO")
    private final String estado;

    @Schema(description = "Cuanto queda disponible para reembolsar en el pago", example = "10000.00")
    private final BigDecimal montoDisponible;

    private final LocalDateTime fechaSolicitud;
    private final LocalDateTime fechaResolucion;

    public ReembolsoResponse(UUID codigoReembolso, UUID codigoPago, BigDecimal monto, String estado,
                             BigDecimal montoDisponible, LocalDateTime fechaSolicitud,
                             LocalDateTime fechaResolucion) {
        this.codigoReembolso = codigoReembolso;
        this.codigoPago = codigoPago;
        this.monto = monto;
        this.estado = estado;
        this.montoDisponible = montoDisponible;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaResolucion = fechaResolucion;
    }

    public UUID getCodigoReembolso() { return codigoReembolso; }
    public UUID getCodigoPago() { return codigoPago; }
    public BigDecimal getMonto() { return monto; }
    public String getEstado() { return estado; }
    public BigDecimal getMontoDisponible() { return montoDisponible; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
}
