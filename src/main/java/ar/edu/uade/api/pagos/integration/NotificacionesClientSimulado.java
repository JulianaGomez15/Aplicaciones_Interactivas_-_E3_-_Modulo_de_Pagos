package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Simula al Servicio de Notificaciones: solo deja traza en el log.
@Component
public class NotificacionesClientSimulado implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientSimulado.class);

    @Override
    public void enviarComprobantePago(PagoResponse pago) {
        log.info("[Notificaciones] comprobante del pago {} por {} {}",
                pago.getCodigoPago(), pago.getMonto(), pago.getMoneda());
    }

    @Override
    public void enviarAvisoRechazo(PagoResponse pago, String motivo) {
        log.info("[Notificaciones] aviso de rechazo del pago {}: {}", pago.getCodigoPago(), motivo);
    }

    @Override
    public void enviarComprobanteReembolso(PagoResponse pago, ReembolsoResponse reembolso) {
        log.info("[Notificaciones] comprobante del reembolso {} ({}) del pago {}",
                reembolso.getCodigoReembolso(), reembolso.getMonto(), pago.getCodigoPago());
    }
}
