package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;

// INT-03: POST /api/v1/notifications en el Servicio de Notificaciones.
public interface NotificacionesClient {

    void enviarComprobantePago(PagoResponse pago);

    void enviarAvisoRechazo(PagoResponse pago, String motivo);

    void enviarComprobanteReembolso(PagoResponse pago, ReembolsoResponse reembolso);
}
