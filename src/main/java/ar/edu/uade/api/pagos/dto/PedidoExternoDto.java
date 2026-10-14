package ar.edu.uade.api.pagos.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Vista de un pedido tal como lo informa el Servicio de Pedidos (modulo vecino).
public class PedidoExternoDto {

    private final UUID codigoPedido;
    private final BigDecimal total;
    private final String estado;

    public PedidoExternoDto(UUID codigoPedido, BigDecimal total, String estado) {
        this.codigoPedido = codigoPedido;
        this.total = total;
        this.estado = estado;
    }

    public UUID getCodigoPedido() { return codigoPedido; }
    public BigDecimal getTotal() { return total; }
    public String getEstado() { return estado; }
}
