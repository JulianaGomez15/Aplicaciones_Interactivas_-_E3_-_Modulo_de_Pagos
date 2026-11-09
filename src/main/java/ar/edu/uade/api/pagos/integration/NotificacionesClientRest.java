package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

// INT-03 real: POST /api/v1/notifications en el Servicio de Notificaciones.
// No captura errores a proposito: NotificacionesGateway los registra en el log y el pago no se revierte (DEC-07).
@Component
@ConditionalOnProperty(name = "integraciones.modo", havingValue = "rest")
public class NotificacionesClientRest implements NotificacionesClient {

    private final RestClient restClient;

    public NotificacionesClientRest(@Qualifier("notificacionesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void enviarComprobantePago(PagoResponse pago) {
        enviar(new NotificacionJson("COMPROBANTE_PAGO", pago.getCodigoPago(), pago.getCodigoPedido(),
                pago.getMonto(), pago.getMoneda(), null));
    }

    @Override
    public void enviarAvisoRechazo(PagoResponse pago, String motivo) {
        enviar(new NotificacionJson("AVISO_RECHAZO", pago.getCodigoPago(), pago.getCodigoPedido(),
                pago.getMonto(), pago.getMoneda(), motivo));
    }

    @Override
    public void enviarComprobanteReembolso(PagoResponse pago, ReembolsoResponse reembolso) {
        enviar(new NotificacionJson("COMPROBANTE_REEMBOLSO", pago.getCodigoPago(), pago.getCodigoPedido(),
                reembolso.getMonto(), pago.getMoneda(), null));
    }

    private void enviar(NotificacionJson notificacion) {
        restClient.post()
                .uri("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(notificacion)
                .retrieve()
                .toBodilessEntity();    // solo nos importa que responda 2xx; si no, lanza excepcion
    }

    // Cuerpo que le mandamos a Notificaciones. Hay que alinearlo con su swagger en la integracion total.
    record NotificacionJson(String tipo, UUID codigoPago, UUID codigoPedido,
                            BigDecimal monto, String moneda, String motivo) {
    }
}
