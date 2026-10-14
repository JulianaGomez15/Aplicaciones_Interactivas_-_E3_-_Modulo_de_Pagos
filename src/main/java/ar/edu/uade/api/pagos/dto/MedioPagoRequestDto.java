package ar.edu.uade.api.pagos.dto;

import java.util.UUID;

// Alta de medio de pago: "detalle" llega en claro (ej. numero de tarjeta simulado) y se enmascara antes de guardar.
public class MedioPagoRequestDto {

    private UUID codigoUsuario;
    private String tipo;
    private String detalle;

    public MedioPagoRequestDto() {
    }

    public MedioPagoRequestDto(UUID codigoUsuario, String tipo, String detalle) {
        this.codigoUsuario = codigoUsuario;
        this.tipo = tipo;
        this.detalle = detalle;
    }

    public UUID getCodigoUsuario() { return codigoUsuario; }
    public void setCodigoUsuario(UUID codigoUsuario) { this.codigoUsuario = codigoUsuario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
