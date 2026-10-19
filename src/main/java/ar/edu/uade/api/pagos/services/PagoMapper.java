package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.MedioPagoResumenResponse;
import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import ar.edu.uade.api.pagos.model.MedioPago;
import ar.edu.uade.api.pagos.model.Pago;
import ar.edu.uade.api.pagos.model.Reembolso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Traduccion Entity -> DTO. Solo lo usan los Services: ninguna Entity cruza hacia el Controller.
@Component
class PagoMapper {

    PagoResponse toResponse(Pago p) {
        MedioPago m = p.getMedioPago();
        return new PagoResponse(p.getCodigoPago(), p.getCodigoPedido(), p.getEstado().name(), p.getMonto(),
                p.getMoneda(), new MedioPagoResumenResponse(m.getTipo().name(), m.getDetalleEnmascarado()),
                p.getFechaCreacion(), p.getFechaActualizacion());
    }

    ReembolsoResponse toResponse(Reembolso r, BigDecimal montoDisponible) {
        return new ReembolsoResponse(r.getCodigoReembolso(), r.getPago().getCodigoPago(), r.getMonto(),
                r.getEstado().name(), montoDisponible, r.getFechaSolicitud(), r.getFechaResolucion());
    }
}
