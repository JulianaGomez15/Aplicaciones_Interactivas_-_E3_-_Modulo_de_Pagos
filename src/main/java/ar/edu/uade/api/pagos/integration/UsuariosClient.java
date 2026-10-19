package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;

import java.util.UUID;

// INT-02: GET /api/v1/users/{codigoUsuario} en el Servicio de Usuarios.
public interface UsuariosClient {

    // Devuelve null si el usuario no existe. Si Usuarios no responde, lanza EXTERNAL_SERVICE_UNAVAILABLE (503).
    UsuarioExternoDto buscarUsuario(UUID codigoUsuario);
}
