package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

// DTO-01
@Schema(description = "Solicitud de pago que envia el Servicio de Pedidos")
public class CrearPagoRequest {

    @NotNull
    @Schema(description = "Codigo del pedido a pagar (debe estar pendiente de pago, estado BORRADOR)",
            example = "00000000-0000-0000-0001-000000000001")
    private UUID codigoPedido;

    @NotNull
    @Schema(description = "Codigo del usuario que paga (debe existir y estar activo)",
            example = "00000000-0000-0000-0000-000000000001")
    private UUID codigoUsuario;

    @NotNull
    @Schema(description = "Codigo del medio de pago (debe estar activo y pertenecer al usuario)")
    private UUID codigoMedioPago;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Importe: debe coincidir exactamente con el total del pedido (RN-02)", example = "15000.00")
    private BigDecimal monto;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}", message = "debe ser un codigo de 3 letras (ej. ARS)")
    @Schema(description = "Moneda (codigo de 3 letras)", example = "ARS")
    private String moneda;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Clave de control (idempotencia, RN-01): la misma clave con los mismos datos devuelve el "
            + "pago existente; con datos distintos responde 409", example = "pedido-0001-intento-1")
    private String claveControl;

    public CrearPagoRequest() {
    }

    public CrearPagoRequest(UUID codigoPedido, UUID codigoUsuario, UUID codigoMedioPago, BigDecimal monto,
                            String moneda, String claveControl) {
        this.codigoPedido = codigoPedido;
        this.codigoUsuario = codigoUsuario;
        this.codigoMedioPago = codigoMedioPago;
        this.monto = monto;
        this.moneda = moneda;
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
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getClaveControl() { return claveControl; }
    public void setClaveControl(String claveControl) { this.claveControl = claveControl; }
}
