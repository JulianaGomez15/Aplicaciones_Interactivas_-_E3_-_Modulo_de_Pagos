package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PedidoExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// Simula al Servicio de Pedidos con un catalogo fijo (BL: mocks mientras no esten los otros modulos).
@Component
public class PedidosClientSimulado implements PedidosClient {

    public static final UUID PEDIDO_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID PEDIDO_2 = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID PEDIDO_3 = UUID.fromString("00000000-0000-0000-0001-000000000003");
    public static final UUID PEDIDO_CANCELADO = UUID.fromString("00000000-0000-0000-0001-000000000004");
    // Simula que Pedidos no responde a tiempo.
    public static final UUID PEDIDO_SIN_RESPUESTA = UUID.fromString("00000000-0000-0000-0001-000000000099");

    private static final Map<UUID, PedidoExternoDto> PEDIDOS = Map.of(
            PEDIDO_1, new PedidoExternoDto(PEDIDO_1, new BigDecimal("15000.00"), "BORRADOR"),
            PEDIDO_2, new PedidoExternoDto(PEDIDO_2, new BigDecimal("8500.50"), "BORRADOR"),
            PEDIDO_3, new PedidoExternoDto(PEDIDO_3, new BigDecimal("32000.00"), "BORRADOR"),
            PEDIDO_CANCELADO, new PedidoExternoDto(PEDIDO_CANCELADO, new BigDecimal("4200.00"), "CANCELADO")
    );

    @Override
    public PedidoExternoDto buscarPedido(UUID codigoPedido) {
        if (PEDIDO_SIN_RESPUESTA.equals(codigoPedido)) {
            throw new PagoException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "El Servicio de Pedidos no respondio a tiempo");
        }
        return PEDIDOS.get(codigoPedido);
    }
}
