package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ReembolsoRequest;
import ar.edu.uade.api.pagos.dto.ReembolsoResponse;
import ar.edu.uade.api.pagos.dto.ResultadoOperacion;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.NotificacionesGateway;
import ar.edu.uade.api.pagos.model.EstadoPago;
import ar.edu.uade.api.pagos.model.HistorialEstadoPago;
import ar.edu.uade.api.pagos.model.Pago;
import ar.edu.uade.api.pagos.model.Reembolso;
import ar.edu.uade.api.pagos.repository.HistorialEstadoPagoRepository;
import ar.edu.uade.api.pagos.repository.PagoRepository;
import ar.edu.uade.api.pagos.repository.ReembolsoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReembolsoService {

    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final HistorialEstadoPagoRepository historialRepository;
    private final NotificacionesGateway notificaciones;
    private final PagoMapper mapper;

    public ReembolsoService(PagoRepository pagoRepository,
                            ReembolsoRepository reembolsoRepository,
                            HistorialEstadoPagoRepository historialRepository,
                            NotificacionesGateway notificaciones,
                            PagoMapper mapper) {
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.historialRepository = historialRepository;
        this.notificaciones = notificaciones;
        this.mapper = mapper;
    }

    // SRV-05 (CU-05)
    @Transactional
    public ResultadoOperacion<ReembolsoResponse> reembolsarPago(UUID codigoPago, ReembolsoRequest dto) {

        validarSolicitud(dto);
        BigDecimal monto = normalizarMonto(dto.getMonto());

        // Con bloqueo: dos reembolsos simultaneos sobre el mismo pago se ejecutan de a uno.
        Pago pago = pagoRepository.buscarParaActualizar(codigoPago);
        if (Objects.isNull(pago)) {
            throw new PagoException(ErrorCode.PAYMENT_NOT_FOUND, "No existe un pago con el codigo " + codigoPago);
        }

        BigDecimal reembolsado = reembolsoRepository.sumarMontosPorPago(pago);

        // RN-10: clave de control ya usada -> se devuelve el reembolso existente.
        Reembolso existente = reembolsoRepository.findByClaveControl(dto.getClaveControl());
        if (Objects.nonNull(existente)) {
            if (!existente.getPago().getId().equals(pago.getId()) || existente.getMonto().compareTo(monto) != 0) {
                throw new PagoException(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                        "La clave de control del reembolso ya fue usada con datos distintos");
            }
            return new ResultadoOperacion<>(
                    mapper.toResponse(existente, pago.getMonto().subtract(reembolsado)), false);
        }

        // RN-06: solo se reembolsa un pago APROBADO.
        if (!pago.puedeReembolsarse()) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "Solo se puede reembolsar un pago APROBADO (estado actual: " + pago.getEstado() + ")");
        }

        // RN-07: la suma de reembolsos no puede superar el monto aprobado.
        BigDecimal disponible = pago.getMonto().subtract(reembolsado);
        if (monto.compareTo(disponible) > 0) {
            throw new PagoException(ErrorCode.REFUND_EXCEEDS_AMOUNT,
                    "El monto " + monto + " supera lo disponible para reembolsar (" + disponible + ")");
        }

        Reembolso reembolso = new Reembolso(monto, dto.getMotivo().trim(), dto.getClaveControl());
        pago.agregarReembolso(reembolso);
        reembolsoRepository.save(reembolso);

        BigDecimal restante = disponible.subtract(monto);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            EstadoPago anterior = pago.getEstado();
            pago.marcarReembolsado();
            HistorialEstadoPago registro = new HistorialEstadoPago(anterior, pago.getEstado(),
                    "Reembolso total: " + reembolso.getMotivo());
            pago.agregarHistorial(registro);
            historialRepository.save(registro);
            pagoRepository.save(pago);
        }

        ReembolsoResponse respuesta = mapper.toResponse(reembolso, restante);
        PagoResponse pagoResponse = mapper.toResponse(pago);
        notificaciones.comprobanteReembolso(pagoResponse, respuesta);
        return new ResultadoOperacion<>(respuesta, true);
    }

    private void validarSolicitud(ReembolsoRequest dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getMotivo()) || dto.getMotivo().isBlank()
                || Objects.isNull(dto.getClaveControl()) || dto.getClaveControl().isBlank()) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "El monto, el motivo y la clave de control son obligatorios");
        }
    }

    private BigDecimal normalizarMonto(BigDecimal monto) {
        if (Objects.isNull(monto) || monto.signum() <= 0) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "El monto debe ser mayor que cero");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "El monto admite como maximo 2 decimales");
        }
        return monto.setScale(2);
    }
}
