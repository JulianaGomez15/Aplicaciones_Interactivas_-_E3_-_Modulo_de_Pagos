package ar.edu.uade.api.pagos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class MedioPagoDto {

    private final UUID codigo;
    private final UUID codigoUsuario;
    private final String tipo;
    private final String detalleEnmascarado;
    private final boolean activo;
    private final LocalDateTime fechaCreacion;

    public MedioPagoDto(UUID codigo, UUID codigoUsuario, String tipo, String detalleEnmascarado, boolean activo,
                        LocalDateTime fechaCreacion) {
        this.codigo = codigo;
        this.codigoUsuario = codigoUsuario;
        this.tipo = tipo;
        this.detalleEnmascarado = detalleEnmascarado;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public UUID getCodigo() { return codigo; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public String getTipo() { return tipo; }
    public String getDetalleEnmascarado() { return detalleEnmascarado; }
    public boolean isActivo() { return activo; }
}
