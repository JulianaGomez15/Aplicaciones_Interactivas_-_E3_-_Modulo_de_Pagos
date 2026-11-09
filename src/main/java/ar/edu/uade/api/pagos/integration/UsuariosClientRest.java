package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

// INT-02 real: GET /api/v1/users/{codigoUsuario} en el Servicio de Usuarios.
@Component
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class UsuariosClientRest implements UsuariosClient {

    private final RestClient restClient;

    public UsuariosClientRest(@Qualifier("usuariosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public UsuarioExternoDto buscarUsuario(UUID codigoUsuario) {
        try {
            UsuarioJson json = restClient.get()
                    .uri("/api/v1/users/{codigoUsuario}", codigoUsuario)
                    .retrieve()
                    .body(UsuarioJson.class);
            return json == null ? null
                    : new UsuarioExternoDto(json.codigoUsuario(), json.nombre(), Boolean.TRUE.equals(json.activo()));

        } catch (HttpClientErrorException.NotFound e) {
            return null;    // el usuario no existe -> PagoService responde 422 PAYMENT_USER_INVALID
        } catch (RestClientException e) {
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Usuarios no respondio correctamente");
        }
    }

    // Forma del JSON tal como lo devuelve Usuarios. Si cambian los nombres de campo, se ajusta solo aca.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsuarioJson(UUID codigoUsuario, String nombre, Boolean activo) {
    }
}
