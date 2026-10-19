package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

// Simula al Servicio de Usuarios con un padron fijo.
@Component
public class UsuariosClientSimulado implements UsuariosClient {

    public static final UUID USUARIO_ANA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USUARIO_BRUNO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID USUARIO_INACTIVO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    // Simula que Usuarios no responde a tiempo.
    public static final UUID USUARIO_SIN_RESPUESTA = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private static final Map<UUID, UsuarioExternoDto> USUARIOS = Map.of(
            USUARIO_ANA, new UsuarioExternoDto(USUARIO_ANA, "Ana Perez", true),
            USUARIO_BRUNO, new UsuarioExternoDto(USUARIO_BRUNO, "Bruno Diaz", true),
            USUARIO_INACTIVO, new UsuarioExternoDto(USUARIO_INACTIVO, "Carla Lopez (inactiva)", false)
    );

    @Override
    public UsuarioExternoDto buscarUsuario(UUID codigoUsuario) {
        if (USUARIO_SIN_RESPUESTA.equals(codigoUsuario)) {
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Usuarios no respondio a tiempo");
        }
        return USUARIOS.get(codigoUsuario);
    }
}
