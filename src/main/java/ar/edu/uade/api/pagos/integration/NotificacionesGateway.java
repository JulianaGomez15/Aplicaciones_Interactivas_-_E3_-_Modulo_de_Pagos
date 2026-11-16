package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.consola.Consola;
import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// DEC-07: si Notificaciones falla, la operacion de pago no se revierte; el error queda en el log.
// Los Services llaman a este gateway despues del commit, nunca con la transaccion abierta.
@Component
public class NotificacionesGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesGateway.class);
    private static final String ETAPA = "NOTIFICACION";

    private final NotificacionesClient cliente;
    private final Consola consola;

    public NotificacionesGateway(NotificacionesClient cliente, Consola consola) {
        this.cliente = cliente;
        this.consola = consola;
    }

    public void comprobantePago(PagoResponse pago) {
        ejecutar("Comprobante de pago", () -> cliente.enviarComprobantePago(pago));
    }

    public void avisoRechazo(PagoResponse pago, String motivo) {
        ejecutar("Aviso de pago no aprobado (" + motivo + ")", () -> cliente.enviarAvisoRechazo(pago, motivo));
    }

    public void comprobanteReembolso(PagoResponse pago, ReembolsoResponse reembolso) {
        ejecutar("Comprobante de reembolso por " + reembolso.getMonto() + " " + pago.getMoneda(),
                () -> cliente.enviarComprobanteReembolso(pago, reembolso));
    }

    private void ejecutar(String que, Runnable envio) {
        try {
            envio.run();
            consola.ok(ETAPA, que + " enviado al usuario");
        } catch (RuntimeException e) {
            consola.fallo(ETAPA, que + ": Notificaciones no respondio (la operacion se mantiene)");
            log.error("Notificaciones no respondio ({}); la operacion de pago se mantiene: {}", que, e.getMessage());
        }
    }
}
