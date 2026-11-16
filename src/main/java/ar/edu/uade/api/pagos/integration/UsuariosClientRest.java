package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

// INT-02 real: GET /api/v1/users/{codigoUsuario} en el Servicio de Usuarios.
@Component
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class UsuariosClientRest implements UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClientRest.class);

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
            return json == null ? null : new UsuarioExternoDto(
                    json.codigoUsuario() == null ? codigoUsuario : json.codigoUsuario(), json.nombre(), json.estaActivo());

        } catch (HttpClientErrorException.NotFound e) {
            return null;    // el usuario no existe -> PagoService responde 422 PAYMENT_USER_INVALID
        } catch (RestClientResponseException e) {
            // Usuarios respondio, pero con error (400, 401, 403, 5xx): se deja la causa en el log.
            log.warn("Usuarios respondio {} a GET /api/v1/users/{}: {}", e.getStatusCode(), codigoUsuario,
                    e.getResponseBodyAsString());
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Usuarios no respondio correctamente");
        } catch (RestClientException e) {
            log.warn("No se pudo consultar a Usuarios (GET /api/v1/users/{}): {}", codigoUsuario, e.getMessage());
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Usuarios no respondio correctamente");
        }
    }

    // Forma del JSON tal como lo devuelve Usuarios. Su swagger informa estado: "ACTIVO"; tambien se acepta
    // activo: true. Si cambian los nombres de campo, se ajusta solo aca.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsuarioJson(UUID codigoUsuario, String nombre, Boolean activo, String estado) {

        boolean estaActivo() {
            return activo != null ? activo : "ACTIVO".equalsIgnoreCase(estado);
        }
    }
}
