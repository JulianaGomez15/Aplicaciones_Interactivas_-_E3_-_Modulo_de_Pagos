package ar.edu.uade.api.pagos.dto;

import java.math.BigDecimal;
import java.util.UUID;

// DTO-01: solicitud de pago que envia el Servicio de Pedidos.
public class IniciarPagoRequestDto {

    private UUID codigoPedido;
    private UUID codigoUsuario;
    private UUID codigoMedioPago;
    private BigDecimal monto;
    private String claveControl;

    public IniciarPagoRequestDto() {
    }

    public IniciarPagoRequestDto(UUID codigoPedido, UUID codigoUsuario, UUID codigoMedioPago, BigDecimal monto,
                                 String claveControl) {
        this.codigoPedido = codigoPedido;
        this.codigoUsuario = codigoUsuario;
        this.codigoMedioPago = codigoMedioPago;
        this.monto = monto;
        this.claveControl = claveControl;
    }

    public UUID getCodigoPedido() { return codigoPedido; }
    public void setCodigoPedido(UUID codigoPedido) { this.codigoPedido = codigoPedido; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public void setCodigoUsuario(UUID codigoUsuario) { this.codigoUsuario = codigoUsuario; }
    public UUID getCodigoMedioPago() { return codigoMedioPago; }
    public void setCodigoMedioPago(UUID codigoMedioPago) { this.codigoMedioPago = codigoMedioPago; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getClaveControl() { return claveControl; }
    public void setClaveControl(String claveControl) { this.claveControl = claveControl; }
}
