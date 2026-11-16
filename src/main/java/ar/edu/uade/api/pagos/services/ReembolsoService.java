package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.consola.Consola;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReembolsoService {

    // Lo que sale de la transaccion: el reembolso, si se creo (false = clave repetida) y el pago ya actualizado.
    private record Registro(ReembolsoResponse reembolso, boolean creado, PagoResponse pago) {
    }

    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final HistorialEstadoPagoRepository historialRepository;
    private final NotificacionesGateway notificaciones;
    private final PagoMapper mapper;
    private final Consola consola;
    private final TransactionTemplate transaccion;

    public ReembolsoService(PagoRepository pagoRepository,
                            ReembolsoRepository reembolsoRepository,
                            HistorialEstadoPagoRepository historialRepository,
                            NotificacionesGateway notificaciones,
                            PagoMapper mapper,
                            Consola consola,
                            PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.historialRepository = historialRepository;
        this.notificaciones = notificaciones;
        this.mapper = mapper;
        this.consola = consola;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // SRV-05 (CU-05). La notificacion se envia despues del commit: el bloqueo del pago no se mantiene
    // mientras se espera a Notificaciones.
    public ResultadoOperacion<ReembolsoResponse> reembolsarPago(UUID codigoPago, ReembolsoRequest dto) {

        // @Valid ya garantizo monto > 0 y a lo sumo 2 decimales; se normaliza la escala para comparar y persistir.
        BigDecimal monto = dto.getMonto().setScale(2);

        consola.titulo("REEMBOLSAR PAGO");
        consola.paso("SOLICITUD", "Llego el pedido de reembolso: pago " + codigoPago + " por " + monto);
        consola.paso("SOLICITUD", "Motivo: " + dto.getMotivo().trim() + " | clave de control " + dto.getClaveControl());

        try {
            Registro registro = transaccion.execute(status -> registrar(codigoPago, dto, monto));

            if (!registro.creado()) {
                consola.ok("IDEMPOTENCIA", "La clave de control ya se uso: se devuelve el reembolso "
                        + registro.reembolso().getCodigoReembolso());
                consola.resultado(true, "Reembolso existente, no se crea otro");
                return new ResultadoOperacion<>(registro.reembolso(), false);
            }

            notificaciones.comprobanteReembolso(registro.pago(), registro.reembolso());
            consola.resultado(true, "Reembolso " + registro.reembolso().getEstado() + " por " + monto
                    + " | pago " + registro.pago().getEstado());
            return new ResultadoOperacion<>(registro.reembolso(), true);

        } catch (RuntimeException e) {
            String detalle = e instanceof PagoException pe ? pe.getCode() + ": " + pe.getMessage()
                    : e.getClass().getSimpleName();
            consola.fallo("ERROR", detalle);
            consola.resultado(false, "No se realizo el reembolso");
            throw e;
        }
    }

    private Registro registrar(UUID codigoPago, ReembolsoRequest dto, BigDecimal monto) {

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
            return new Registro(mapper.toResponse(existente, pago.getMonto().subtract(reembolsado)), false, null);
        }

        // RN-06: solo se reembolsa un pago APROBADO.
        if (!pago.puedeReembolsarse()) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "Solo se puede reembolsar un pago APROBADO (estado actual: " + pago.getEstado() + ")");
        }
        consola.ok("VALIDACION", "El pago esta APROBADO");

        // RN-07: la suma de reembolsos no puede superar el monto aprobado.
        BigDecimal disponible = pago.getMonto().subtract(reembolsado);
        if (monto.compareTo(disponible) > 0) {
            throw new PagoException(ErrorCode.REFUND_EXCEEDS_AMOUNT,
                    "El monto " + monto + " supera lo disponible para reembolsar (" + disponible + ")");
        }
        consola.ok("VALIDACION", "El monto no supera lo disponible para reembolsar (" + disponible + ")");

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
            pagoRepository.saveAndFlush(pago);
            consola.ok("REGISTRO", "Reembolso total: el pago pasa a REEMBOLSADO");
        } else {
            consola.ok("REGISTRO", "Reembolso parcial: el pago sigue APROBADO, quedan " + restante + " disponibles");
        }

        return new Registro(mapper.toResponse(reembolso, restante), true, mapper.toResponse(pago));
    }
}
