package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;

import java.util.UUID;

// Contrato contra el Servicio de Usuarios y Autenticacion (INT-02).
public interface UsuariosClient {

    // Devuelve null si el usuario no existe.
    UsuarioExternoDto buscarUsuario(UUID codigoUsuario);
}
