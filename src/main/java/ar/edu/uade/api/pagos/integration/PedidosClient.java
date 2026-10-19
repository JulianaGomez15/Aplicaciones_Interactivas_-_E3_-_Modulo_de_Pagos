package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PedidoExternoDto;

import java.util.UUID;

// INT-01: GET /api/v1/orders/{codigoPedido} en el Servicio de Pedidos.
// Pagos no avisa a Pedidos: el resultado le llega en la respuesta de approve / reject.
public interface PedidosClient {

    // Devuelve null si el pedido no existe. Si Pedidos no responde, lanza EXTERNAL_SERVICE_UNAVAILABLE (503).
    PedidoExternoDto buscarPedido(UUID codigoPedido);
}
