package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.PedidoExternoDto;

import java.util.UUID;

// INT-01: GET /api/v1/orders/{id} en el Servicio de Pedidos, mas el aviso del resultado del pago.
public interface PedidosClient {

    // Devuelve null si el pedido no existe. Si Pedidos no responde, lanza EXTERNAL_SERVICE_UNAVAILABLE (503).
    // codigoUsuario viaja en el header X-Cliente-Id, que Pedidos exige en esa consulta.
    PedidoExternoDto buscarPedido(UUID codigoPedido, UUID codigoUsuario);

    // Le avisa a Pedidos como termino el pago. Devuelve false si no hay un endpoint de aviso configurado
    // (en ese caso Pedidos se entera por la respuesta de la API). Si la llamada falla, lanza la excepcion.
    boolean avisarResultadoPago(PagoResponse pago);
}
