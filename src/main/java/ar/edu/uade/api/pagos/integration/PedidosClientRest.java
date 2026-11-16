package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.PedidoExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

// INT-01 real: GET /api/v1/orders/{id} en el Servicio de Pedidos.
@Component
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class PedidosClientRest implements PedidosClient {

    private static final Logger log = LoggerFactory.getLogger(PedidosClientRest.class);

    // Header que Pedidos exige en sus endpoints (swagger del ecosistema).
    static final String HEADER_CLIENTE = "X-Cliente-Id";

    private final RestClient restClient;
    private final String rutaAviso;

    public PedidosClientRest(@Qualifier("pedidosRestClient") RestClient restClient,
                             @Value("${integraciones.pedidos.ruta-aviso:}") String rutaAviso) {
        this.restClient = restClient;
        this.rutaAviso = rutaAviso == null ? "" : rutaAviso.trim();
    }

    @Override
    public PedidoExternoDto buscarPedido(UUID codigoPedido, UUID codigoUsuario) {
        try {
            PedidoJson json = restClient.get()
                    .uri("/api/v1/orders/{id}", codigoPedido)
                    .header(HEADER_CLIENTE, codigoUsuario.toString())
                    .retrieve()
                    .body(PedidoJson.class);
            return json == null ? null : new PedidoExternoDto(
                    json.codigoPedido() == null ? codigoPedido : json.codigoPedido(), json.total(), json.estado());

        } catch (HttpClientErrorException.NotFound e) {
            return null;    // el pedido no existe (o es de otro cliente) -> PagoService responde 422 ORDER_NOT_PAYABLE
        } catch (RestClientResponseException e) {
            // Pedidos respondio, pero con error (400, 401, 403, 5xx): se deja la causa en el log.
            log.warn("Pedidos respondio {} a GET /api/v1/orders/{}: {}", e.getStatusCode(), codigoPedido,
                    e.getResponseBodyAsString());
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Pedidos no respondio correctamente");
        } catch (RestClientException e) {
            // timeout, servicio caido o respuesta ilegible -> 503, no se crea el pago (INT-01)
            log.warn("No se pudo consultar a Pedidos (GET /api/v1/orders/{}): {}", codigoPedido, e.getMessage());
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Pedidos no respondio correctamente");
        }
    }

    // El swagger de Pedidos todavia no define el endpoint que recibe este aviso: la ruta se configura en
    // integraciones.pedidos.ruta-aviso (admite {codigoPedido}). Vacia = no se llama.
    @Override
    public boolean avisarResultadoPago(PagoResponse pago) {
        if (rutaAviso.isEmpty()) {
            return false;
        }
        try {
            restClient.post()
                    .uri(rutaAviso, pago.getCodigoPedido())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AvisoPagoJson(pago.getCodigoPago(), pago.getCodigoPedido(), pago.getEstado(),
                            pago.getMotivo(), pago.getMonto(), pago.getMoneda()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.warn("Pedidos respondio {} al aviso del pago {}: {}", e.getStatusCode(), pago.getCodigoPago(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    // Forma del JSON tal como lo devuelve Pedidos (su campo se llama "id"). Si cambian los nombres, se ajusta aca.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PedidoJson(@JsonAlias("id") UUID codigoPedido, BigDecimal total, String estado) {
    }

    // Cuerpo del aviso a Pedidos. Hay que alinearlo con su swagger cuando definan el endpoint.
    record AvisoPagoJson(UUID codigoPago, UUID codigoPedido, String estado, String motivo,
                         BigDecimal monto, String moneda) {
    }
}
