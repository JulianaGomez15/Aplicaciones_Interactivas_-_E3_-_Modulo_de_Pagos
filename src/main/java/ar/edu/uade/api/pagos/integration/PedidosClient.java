package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PedidoExternoDto;

import java.util.UUID;

// Contrato contra el Servicio de Pedidos (INT-01 / INT-04).
public interface PedidosClient {

    // Devuelve null si el pedido no existe.
    PedidoExternoDto buscarPedido(UUID codigoPedido);

    void notificarPagoAprobado(UUID codigoPedido, UUID codigoPago);

    void notificarPagoRechazado(UUID codigoPedido, UUID codigoPago);

    void notificarPagoReembolsado(UUID codigoPedido, UUID codigoPago);
}
