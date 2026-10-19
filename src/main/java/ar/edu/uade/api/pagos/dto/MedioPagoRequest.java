package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO-07
@Schema(description = "Alta de medio de pago. El usuario se toma del JWT, no del cuerpo.")
public class MedioPagoRequest {

    @NotBlank
    @Schema(allowableValues = {"TARJETA_CREDITO", "TARJETA_DEBITO", "BILLETERA_VIRTUAL", "TRANSFERENCIA"},
            example = "TARJETA_CREDITO")
    private String tipo;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Numero simulado (13 a 19 digitos para tarjetas). Solo viaja en esta solicitud: se guardan "
            + "unicamente los ultimos 4 y el resto se descarta (RN-08)", example = "4111 1111 1111 1234")
    private String numero;

    public MedioPagoRequest() {
    }

    public MedioPagoRequest(String tipo, String numero) {
        this.tipo = tipo;
        this.numero = numero;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
}
