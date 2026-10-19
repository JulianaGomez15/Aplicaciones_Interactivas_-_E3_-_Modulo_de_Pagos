package ar.edu.uade.api.pagos.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Cuentas de demo que simulan al Servicio de Usuarios y Autenticacion (unico emisor real del JWT).
public final class CuentasDemo {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ANALISTA = "ANALISTA";
    public static final String CLIENTE = "CLIENTE";
    public static final String SERVICIO_ESCRITURA = "SERVICIO_ESCRITURA";
    public static final String SERVICIO_LECTURA = "SERVICIO_LECTURA";

    public record Cuenta(String username, String password, List<String> roles, UUID codigoUsuario) {
    }

    public static final List<Cuenta> CUENTAS = List.of(
            new Cuenta("admin", "admin123", List.of(ADMINISTRADOR), null),
            new Cuenta("analista", "analista123", List.of(ANALISTA), null),
            // Los CLIENTE coinciden con los usuarios simulados de Usuarios (Ana y Bruno).
            new Cuenta("cliente", "cliente123", List.of(CLIENTE),
                    UUID.fromString("00000000-0000-0000-0000-000000000001")),
            new Cuenta("cliente2", "cliente123", List.of(CLIENTE),
                    UUID.fromString("00000000-0000-0000-0000-000000000002")),
            // JWT de servicio de Pedidos: escribe y lee.
            new Cuenta("api-pedidos", "pedidos123", List.of(SERVICIO_ESCRITURA, SERVICIO_LECTURA), null),
            // JWT de servicio de Analitica: solo lee.
            new Cuenta("api-analitica", "analitica123", List.of(SERVICIO_LECTURA), null));

    private static final Map<String, Cuenta> POR_USERNAME =
            Map.copyOf(CUENTAS.stream().collect(java.util.stream.Collectors.toMap(Cuenta::username, c -> c)));

    public static Cuenta porUsername(String username) {
        return POR_USERNAME.get(username);
    }

    private CuentasDemo() {
    }
}
