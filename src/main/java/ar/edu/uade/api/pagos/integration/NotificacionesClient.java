package ar.edu.uade.api.pagos.integration;

import ar.edu.uade.api.pagos.dto.PagoDto;
import ar.edu.uade.api.pagos.dto.ReembolsoDto;

// Contrato contra el Servicio de Notificaciones (INT-03).
public interface NotificacionesClient {

    void enviarComprobantePago(PagoDto pago);

    void enviarAvisoRechazo(PagoDto pago, String motivo);

    void enviarComprobanteReembolso(PagoDto pago, ReembolsoDto reembolso);
}
