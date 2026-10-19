package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.NotificacionesGateway;
import ar.edu.uade.api.pagos.integration.PedidosClient;
import ar.edu.uade.api.pagos.integration.UsuariosClient;
import ar.edu.uade.api.pagos.model.EstadoPago;
import ar.edu.uade.api.pagos.model.HistorialEstadoPago;
import ar.edu.uade.api.pagos.model.MedioPago;
import ar.edu.uade.api.pagos.model.Pago;
import ar.edu.uade.api.pagos.repository.HistorialEstadoPagoRepository;
import ar.edu.uade.api.pagos.repository.MedioPagoRepository;
import ar.edu.uade.api.pagos.repository.PagoRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PagoService {

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("fechaCreacion", "fechaActualizacion", "monto", "estado");

    private final PagoRepository pagoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final HistorialEstadoPagoRepository historialRepository;
    private final UsuariosClient usuariosClient;
    private final PedidosClient pedidosClient;
    private final NotificacionesGateway notificaciones;
    private final PagoMapper mapper;
    private final TransactionTemplate transaccion;

    public PagoService(PagoRepository pagoRepository,
                       MedioPagoRepository medioPagoRepository,
                       HistorialEstadoPagoRepository historialRepository,
                       UsuariosClient usuariosClient,
                       PedidosClient pedidosClient,
                       NotificacionesGateway notificaciones,
                       PagoMapper mapper,
                       PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.historialRepository = historialRepository;
        this.usuariosClient = usuariosClient;
        this.pedidosClient = pedidosClient;
        this.notificaciones = notificaciones;
        this.mapper = mapper;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------ SRV-01 (CU-01 / CU-09)

    // Sin @Transactional a proposito: si dos solicitudes simultaneas con la misma clave chocan con el UNIQUE
    // de clave_control, la transaccion perdedora se revierte y se vuelve a leer, ya en otra transaccion, el pago
    // creado por la ganadora (CU-09 4b).
    public ResultadoOperacion<PagoResponse> iniciarPago(CrearPagoRequest dto) {

        validarSolicitud(dto);
        BigDecimal monto = normalizarMonto(dto.getMonto());

        try {
            return transaccion.execute(status -> registrarPago(dto, monto));
        } catch (DataIntegrityViolationException e) {
            return transaccion.execute(status -> {
                Pago ganador = pagoRepository.findByClaveControl(dto.getClaveControl());
                if (Objects.isNull(ganador)) {
                    throw e;
                }
                return respuestaIdempotente(ganador, dto, monto);
            });
        }
    }

    private ResultadoOperacion<PagoResponse> registrarPago(CrearPagoRequest dto, BigDecimal monto) {

        // RN-01: misma clave -> pago existente (mismos datos) o 409 (datos distintos).
        Pago existente = pagoRepository.findByClaveControl(dto.getClaveControl());
        if (Objects.nonNull(existente)) {
            return respuestaIdempotente(existente, dto, monto);
        }

        // Dependencias externas: el usuario, el medio de pago y el pedido deben ser validos.
        UsuarioExternoDto usuario = usuariosClient.buscarUsuario(dto.getCodigoUsuario());
        if (Objects.isNull(usuario) || !usuario.isActivo()) {
            throw new PagoException(ErrorCode.PAYMENT_USER_INVALID,
                    "El usuario no existe o esta inactivo: " + dto.getCodigoUsuario());
        }

        MedioPago medioPago = medioPagoRepository.findByCodigoMedioPago(dto.getCodigoMedioPago());
        if (Objects.isNull(medioPago) || !medioPago.getActivo()
                || !medioPago.getCodigoUsuario().equals(dto.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "El medio de pago no existe, esta inactivo o pertenece a otro usuario");
        }

        PedidoExternoDto pedido = pedidosClient.buscarPedido(dto.getCodigoPedido());
        if (Objects.isNull(pedido) || !"BORRADOR".equals(pedido.getEstado())) {
            throw new PagoException(ErrorCode.ORDER_NOT_PAYABLE,
                    "El pedido no existe o no esta pendiente de pago: " + dto.getCodigoPedido());
        }

        // RN-02: el importe debe coincidir exactamente con el total del pedido.
        if (pedido.getTotal().compareTo(monto) != 0) {
            throw new PagoException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "El monto " + monto + " no coincide con el total del pedido " + pedido.getTotal());
        }

        Pago pago = new Pago(dto.getCodigoPedido(), dto.getCodigoUsuario(), monto,
                dto.getMoneda().toUpperCase(), dto.getClaveControl(), medioPago);
        HistorialEstadoPago alta = new HistorialEstadoPago(null, EstadoPago.PENDIENTE, "Pago iniciado");
        pago.agregarHistorial(alta);

        pagoRepository.save(pago);
        historialRepository.save(alta);
        return new ResultadoOperacion<>(mapper.toResponse(pago), true);
    }

    private ResultadoOperacion<PagoResponse> respuestaIdempotente(Pago existente, CrearPagoRequest dto,
                                                                  BigDecimal monto) {
        boolean mismosDatos = existente.getCodigoPedido().equals(dto.getCodigoPedido())
                && existente.getCodigoUsuario().equals(dto.getCodigoUsuario())
                && existente.getMedioPago().getCodigoMedioPago().equals(dto.getCodigoMedioPago())
                && existente.getMonto().compareTo(monto) == 0;
        if (!mismosDatos) {
            throw new PagoException(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                    "La clave de control ya fue usada con datos distintos: " + dto.getClaveControl());
        }
        return new ResultadoOperacion<>(mapper.toResponse(existente), false);
    }

    // ------------------------------------------------------------ SRV-02 (CU-02)

    @Transactional
    public PagoResponse aprobarPago(UUID codigoPago) {

        Pago pago = buscarEntidad(codigoPago);

        // RN-10: repetir la operacion no genera efectos.
        if (pago.getEstado() == EstadoPago.APROBADO) {
            return mapper.toResponse(pago);
        }

        EstadoPago anterior = pago.getEstado();
        pago.aprobar(); // valida RN-04
        registrarHistorial(pago, anterior, "Autorizacion simulada aprobada");
        pagoRepository.save(pago);

        PagoResponse respuesta = mapper.toResponse(pago);
        notificaciones.comprobantePago(respuesta);
        return respuesta;
    }

    // ------------------------------------------------------------ SRV-03 (CU-03)

    @Transactional
    public PagoResponse rechazarPago(UUID codigoPago, RechazarPagoRequest dto) {

        Pago pago = buscarEntidad(codigoPago);

        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            return mapper.toResponse(pago);
        }

        String motivo = (Objects.isNull(dto) || Objects.isNull(dto.getMotivo()) || dto.getMotivo().isBlank())
                ? "Rechazo simulado"
                : dto.getMotivo().trim();

        EstadoPago anterior = pago.getEstado();
        pago.rechazar(); // valida RN-04
        registrarHistorial(pago, anterior, motivo);
        pagoRepository.save(pago);

        PagoResponse respuesta = mapper.toResponse(pago);
        notificaciones.avisoRechazo(respuesta, motivo);
        return respuesta;
    }

    // ------------------------------------------------------------ SRV-04 (CU-04)

    // soloDeUsuario != null cuando quien consulta es un CLIENTE: solo puede ver sus propios pagos.
    @Transactional
    public PagoResponse consultarPago(UUID codigoPago, UUID soloDeUsuario) {
        Pago pago = buscarEntidad(codigoPago);
        if (Objects.nonNull(soloDeUsuario) && !soloDeUsuario.equals(pago.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.FORBIDDEN, "El pago pertenece a otro usuario");
        }
        return mapper.toResponse(pago);
    }

    // ------------------------------------------------------------ SRV-06 (CU-06 / CU-07)

    @Transactional
    public PaginaPagosResponse listarPagos(String estado, UUID codigoUsuario, UUID codigoPedido,
                                           LocalDate desde, LocalDate hasta, Pageable pageable) {

        EstadoPago estadoFiltro = parsearEstado(estado);
        if (Objects.nonNull(desde) && Objects.nonNull(hasta) && desde.isAfter(hasta)) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "'desde' no puede ser posterior a 'hasta'");
        }
        for (Sort.Order orden : pageable.getSort()) {
            if (!CAMPOS_ORDENABLES.contains(orden.getProperty())) {
                throw new PagoException(ErrorCode.VALIDATION_ERROR,
                        "No se puede ordenar por '" + orden.getProperty() + "'. Campos validos: " + CAMPOS_ORDENABLES);
            }
        }

        Page<Pago> pagina = pagoRepository.findAll(
                construirFiltro(estadoFiltro, codigoUsuario, codigoPedido, desde, hasta),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()));

        List<PagoResponse> contenido = pagina.getContent().stream().map(mapper::toResponse).toList();
        return new PaginaPagosResponse(contenido, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    // ------------------------------------------------------------ SRV-08 (CU-10)

    @Transactional
    public List<HistorialEstadoResponse> consultarHistorial(UUID codigoPago) {
        buscarEntidad(codigoPago);
        return historialRepository.findByPagoCodigoPagoOrderByFechaAscIdAsc(codigoPago).stream()
                .map(h -> new HistorialEstadoResponse(
                        Objects.isNull(h.getEstadoAnterior()) ? null : h.getEstadoAnterior().name(),
                        h.getEstadoNuevo().name(), h.getMotivo(), h.getFecha()))
                .toList();
    }

    // ------------------------------------------------------------ privados

    private Pago buscarEntidad(UUID codigoPago) {
        Pago pago = pagoRepository.findByCodigoPago(codigoPago);
        if (Objects.isNull(pago)) {
            throw new PagoException(ErrorCode.PAYMENT_NOT_FOUND, "No existe un pago con el codigo " + codigoPago);
        }
        return pago;
    }

    // RN-09: cada cambio de estado deja su rastro.
    private void registrarHistorial(Pago pago, EstadoPago anterior, String motivo) {
        HistorialEstadoPago registro = new HistorialEstadoPago(anterior, pago.getEstado(), motivo);
        pago.agregarHistorial(registro);
        historialRepository.save(registro);
    }

    private void validarSolicitud(CrearPagoRequest dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getCodigoPedido()) || Objects.isNull(dto.getCodigoUsuario())
                || Objects.isNull(dto.getCodigoMedioPago()) || Objects.isNull(dto.getMoneda())
                || Objects.isNull(dto.getClaveControl()) || dto.getClaveControl().isBlank()) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "Faltan campos obligatorios en la solicitud de pago");
        }
    }

    // Monto > 0 y a lo sumo 2 decimales; se normaliza a escala 2 para comparar y persistir.
    private BigDecimal normalizarMonto(BigDecimal monto) {
        if (Objects.isNull(monto) || monto.signum() <= 0) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "El monto debe ser mayor que cero");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "El monto admite como maximo 2 decimales");
        }
        return monto.setScale(2);
    }

    private EstadoPago parsearEstado(String estado) {
        if (Objects.isNull(estado) || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPago.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PagoException(ErrorCode.VALIDATION_ERROR, "Estado invalido: " + estado);
        }
    }

    private Specification<Pago> construirFiltro(EstadoPago estado, UUID codigoUsuario, UUID codigoPedido,
                                                LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            // DEC-06: relaciones LAZY; el medio de pago se trae con fetch join para evitar N+1 (no en el count).
            if (Objects.nonNull(query) && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                root.fetch("medioPago", JoinType.INNER);
            }
            List<Predicate> condiciones = new ArrayList<>();
            if (Objects.nonNull(estado)) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (Objects.nonNull(codigoUsuario)) {
                condiciones.add(cb.equal(root.get("codigoUsuario"), codigoUsuario));
            }
            if (Objects.nonNull(codigoPedido)) {
                condiciones.add(cb.equal(root.get("codigoPedido"), codigoPedido));
            }
            if (Objects.nonNull(desde)) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde.atStartOfDay()));
            }
            if (Objects.nonNull(hasta)) {
                condiciones.add(cb.lessThan(root.get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
