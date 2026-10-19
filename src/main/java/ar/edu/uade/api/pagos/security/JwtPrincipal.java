package ar.edu.uade.api.pagos.security;

import java.util.List;
import java.util.UUID;

// Identidad que Pagos reconstruye localmente a partir del JWT (no consulta a Usuarios en cada request).
public class JwtPrincipal {

    // Codigo que no coincide con ningun usuario: se usa si un token CLIENTE no trae codigoUsuario.
    private static final UUID NINGUNO = new UUID(0L, 0L);

    private final String username;
    private final UUID codigoUsuario;
    private final List<String> roles;

    public JwtPrincipal(String username, UUID codigoUsuario, List<String> roles) {
        this.username = username;
        this.codigoUsuario = codigoUsuario;
        this.roles = roles;
    }

    public String getUsername() { return username; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public List<String> getRoles() { return roles; }

    public boolean esCliente() {
        return roles.contains("CLIENTE");
    }

    // CU-04: un CLIENTE solo ve sus pagos. Devuelve null cuando no hay restriccion (ADMINISTRADOR, servicios).
    public UUID restringirAUsuario() {
        if (!esCliente()) {
            return null;
        }
        return codigoUsuario != null ? codigoUsuario : NINGUNO;
    }

    @Override
    public String toString() {
        return username;
    }
}
