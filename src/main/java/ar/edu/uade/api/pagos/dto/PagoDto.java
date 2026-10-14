package ar.edu.uade.api.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO-02: representacion publica de un pago.
public class PagoDto {

    private final UUID codigo;
    private final UUID codigoPedido;
    private final UUID codigoUsuario;
    private final BigDecimal monto;
    private final String moneda;
    private final String estado;
    private final String claveControl;
    private final MedioPagoDto medioPago;
    private final BigDecimal montoReembolsado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    public PagoDto(UUID codigo, UUID codigoPedido, UUID codigoUsuario, BigDecimal monto, String moneda,
                   String estado, String claveControl, MedioPagoDto medioPago, BigDecimal montoReembolsado,
                   LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.codigo = codigo;
        this.codigoPedido = codigoPedido;
        this.codigoUsuario = codigoUsuario;
        this.monto = monto;
        this.moneda = moneda;
        this.estado = estado;
        this.claveControl = claveControl;
        this.medioPago = medioPago;
        this.montoReembolsado = montoReembolsado;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public UUID getCodigo() { return codigo; }
    public UUID getCodigoPedido() { return codigoPedido; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public String getEstado() { return estado; }
    public String getClaveControl() { return claveControl; }
    public MedioPagoDto getMedioPago() { return medioPago; }
    public BigDecimal getMontoReembolsado() { return montoReembolsado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    @Override
    public String toString() {
        return "PagoDto{codigo=" + codigo + ", estado='" + estado + "', monto=" + monto + "}";
    }
}
