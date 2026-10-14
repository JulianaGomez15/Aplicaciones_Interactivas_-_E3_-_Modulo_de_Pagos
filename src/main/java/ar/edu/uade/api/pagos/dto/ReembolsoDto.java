package ar.edu.uade.api.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO-05: representacion publica de un reembolso.
public class ReembolsoDto {

    private final UUID codigo;
    private final UUID codigoPago;
    private final BigDecimal monto;
    private final String motivo;
    private final String estado;
    private final LocalDateTime fecha;
    private final String estadoPago;
    private final BigDecimal montoDisponible;

    public ReembolsoDto(UUID codigo, UUID codigoPago, BigDecimal monto, String motivo, String estado,
                        LocalDateTime fecha, String estadoPago, BigDecimal montoDisponible) {
        this.codigo = codigo;
        this.codigoPago = codigoPago;
        this.monto = monto;
        this.motivo = motivo;
        this.estado = estado;
        this.fecha = fecha;
        this.estadoPago = estadoPago;
        this.montoDisponible = montoDisponible;
    }

    public UUID getCodigo() { return codigo; }
    public UUID getCodigoPago() { return codigoPago; }
    public BigDecimal getMonto() { return monto; }
    public String getMotivo() { return motivo; }
    public String getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
    // Estado del pago luego del reembolso y saldo que todavia se puede devolver.
    public String getEstadoPago() { return estadoPago; }
    public BigDecimal getMontoDisponible() { return montoDisponible; }
}
