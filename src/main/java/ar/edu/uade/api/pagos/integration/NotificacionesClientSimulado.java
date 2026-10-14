package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoDto;
import ar.edu.uade.api.pagos.dto.ReembolsoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Simula al Servicio de Notificaciones: solo deja traza en el log.
@Component
public class NotificacionesClientSimulado implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientSimulado.class);

    @Override
    public void enviarComprobantePago(PagoDto pago) {
        log.info("[Notificaciones] comprobante de pago {} ({}) al usuario {}",
                pago.getCodigo(), pago.getMonto(), pago.getCodigoUsuario());
    }

    @Override
    public void enviarAvisoRechazo(PagoDto pago, String motivo) {
        log.info("[Notificaciones] aviso de rechazo del pago {} al usuario {}: {}",
                pago.getCodigo(), pago.getCodigoUsuario(), motivo);
    }

    @Override
    public void enviarComprobanteReembolso(PagoDto pago, ReembolsoDto reembolso) {
        log.info("[Notificaciones] comprobante de reembolso {} ({}) del pago {} al usuario {}",
                reembolso.getCodigo(), reembolso.getMonto(), pago.getCodigo(), pago.getCodigoUsuario());
    }
}
