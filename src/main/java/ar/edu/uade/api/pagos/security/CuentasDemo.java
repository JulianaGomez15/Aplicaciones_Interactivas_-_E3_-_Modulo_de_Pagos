package ar.edu.uade.api.pagos.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Cuenta de demo que simula al Servicio de Usuarios y Autenticacion (unico emisor real del JWT).
// Hay un solo usuario para todas las transacciones: tiene todos los roles y es el usuario Ana.
public final class CuentasDemo {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ANALISTA = "ANALISTA";
    public static final String CLIENTE = "CLIENTE";
    public static final String SERVICIO_ESCRITURA = "SERVICIO_ESCRITURA";
    public static final String SERVICIO_LECTURA = "SERVICIO_LECTURA";

    public record Cuenta(String username, String password, List<String> roles, UUID codigoUsuario) {
    }

    // Unica cuenta de demo. Los roles se siguen validando en cada endpoint (SecurityConfig): en el ecosistema
    // real cada JWT que emita Usuarios trae solo los suyos.
    public static final Cuenta DEMO = new Cuenta("demo", "demo123",
            List.of(CLIENTE, SERVICIO_ESCRITURA, SERVICIO_LECTURA, ADMINISTRADOR, ANALISTA),
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    public static final List<Cuenta> CUENTAS = List.of(DEMO);

    private static final Map<String, Cuenta> POR_USERNAME =
            Map.copyOf(CUENTAS.stream().collect(java.util.stream.Collectors.toMap(Cuenta::username, c -> c)));

    public static Cuenta porUsername(String username) {
        return POR_USERNAME.get(username);
    }

    private CuentasDemo() {
    }
}
