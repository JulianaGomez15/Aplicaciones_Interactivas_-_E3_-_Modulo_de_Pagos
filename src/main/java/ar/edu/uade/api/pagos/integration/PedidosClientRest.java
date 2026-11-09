package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PedidoExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

// INT-01 real: GET /api/v1/orders/{codigoPedido} en el Servicio de Pedidos.
@Component
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class PedidosClientRest implements PedidosClient {

    private final RestClient restClient;

    public PedidosClientRest(@Qualifier("pedidosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PedidoExternoDto buscarPedido(UUID codigoPedido) {
        try {
            PedidoJson json = restClient.get()
                    .uri("/api/v1/orders/{codigoPedido}", codigoPedido)
                    .retrieve()
                    .body(PedidoJson.class);
            return json == null ? null : new PedidoExternoDto(json.codigoPedido(), json.total(), json.estado());

        } catch (HttpClientErrorException.NotFound e) {
            return null;    // el pedido no existe -> PagoService responde 422 ORDER_NOT_PAYABLE
        } catch (RestClientException e) {
            // timeout, servicio caido, 5xx o respuesta ilegible -> 503, no se crea el pago (INT-01)
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Pedidos no respondio correctamente");
        }
    }

    // Forma del JSON tal como lo devuelve Pedidos. Si cambian los nombres de campo, se ajusta solo aca.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PedidoJson(UUID codigoPedido, BigDecimal total, String estado) {
    }
}
