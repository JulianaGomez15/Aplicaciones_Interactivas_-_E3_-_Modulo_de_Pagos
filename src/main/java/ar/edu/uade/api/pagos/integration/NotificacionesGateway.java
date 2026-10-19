package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// DEC-07: si Notificaciones falla, la operacion de pago no se revierte; el error queda en el log.
@Component
public class NotificacionesGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesGateway.class);

    private final NotificacionesClient cliente;

    public NotificacionesGateway(NotificacionesClient cliente) {
        this.cliente = cliente;
    }

    public void comprobantePago(PagoResponse pago) {
        ejecutar("comprobante de pago " + pago.getCodigoPago(), () -> cliente.enviarComprobantePago(pago));
    }

    public void avisoRechazo(PagoResponse pago, String motivo) {
        ejecutar("aviso de rechazo " + pago.getCodigoPago(), () -> cliente.enviarAvisoRechazo(pago, motivo));
    }

    public void comprobanteReembolso(PagoResponse pago, ReembolsoResponse reembolso) {
        ejecutar("comprobante de reembolso " + reembolso.getCodigoReembolso(),
                () -> cliente.enviarComprobanteReembolso(pago, reembolso));
    }

    private void ejecutar(String que, Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.error("Notificaciones no respondio ({}); la operacion de pago se mantiene: {}", que, e.getMessage());
        }
    }
}
