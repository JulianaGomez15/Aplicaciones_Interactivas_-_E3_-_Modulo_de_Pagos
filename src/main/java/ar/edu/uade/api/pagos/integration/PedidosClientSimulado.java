package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PedidoExternoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// Simula al Servicio de Pedidos con un catalogo fijo (no hay red ni otro modulo real).
@Component
public class PedidosClientSimulado implements PedidosClient {

    private static final Logger log = LoggerFactory.getLogger(PedidosClientSimulado.class);

    public static final UUID PEDIDO_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID PEDIDO_2 = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID PEDIDO_3 = UUID.fromString("00000000-0000-0000-0001-000000000003");
    public static final UUID PEDIDO_CANCELADO = UUID.fromString("00000000-0000-0000-0001-000000000004");

    private static final Map<UUID, PedidoExternoDto> PEDIDOS = Map.of(
            PEDIDO_1, new PedidoExternoDto(PEDIDO_1, new BigDecimal("15000.00"), "CONFIRMADO"),
            PEDIDO_2, new PedidoExternoDto(PEDIDO_2, new BigDecimal("8500.50"), "CONFIRMADO"),
            PEDIDO_3, new PedidoExternoDto(PEDIDO_3, new BigDecimal("32000.00"), "CONFIRMADO"),
            PEDIDO_CANCELADO, new PedidoExternoDto(PEDIDO_CANCELADO, new BigDecimal("4200.00"), "CANCELADO")
    );

    @Override
    public PedidoExternoDto buscarPedido(UUID codigoPedido) {
        return PEDIDOS.get(codigoPedido);
    }

    @Override
    public void notificarPagoAprobado(UUID codigoPedido, UUID codigoPago) {
        log.info("[Pedidos] pedido {} puede avanzar: pago {} APROBADO", codigoPedido, codigoPago);
    }

    @Override
    public void notificarPagoRechazado(UUID codigoPedido, UUID codigoPago) {
        log.info("[Pedidos] pedido {} sigue sin pago: pago {} RECHAZADO", codigoPedido, codigoPago);
    }

    @Override
    public void notificarPagoReembolsado(UUID codigoPedido, UUID codigoPago) {
        log.info("[Pedidos] pedido {}: pago {} REEMBOLSADO", codigoPedido, codigoPago);
    }
}
