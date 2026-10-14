package ar.edu.uade.api.pagos.dto;

import java.util.UUID;

// Vista de un usuario tal como lo informa el Servicio de Usuarios y Autenticacion (modulo vecino).
public class UsuarioExternoDto {

    private final UUID codigoUsuario;
    private final String nombre;
    private final boolean activo;

    public UsuarioExternoDto(UUID codigoUsuario, String nombre, boolean activo) {
        this.codigoUsuario = codigoUsuario;
        this.nombre = nombre;
        this.activo = activo;
    }

    public UUID getCodigoUsuario() { return codigoUsuario; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
