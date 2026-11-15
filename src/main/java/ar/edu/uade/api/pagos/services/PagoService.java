package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.consola.Consola;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    // Resultado de un cambio de estado: el pago y si realmente cambio (false = operacion repetida, RN-10).
    private record Cambio(PagoResponse pago, boolean cambio) {
    }

    private final PagoRepository pagoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final HistorialEstadoPagoRepository historialRepository;
    private final UsuariosClient usuariosClient;
    private final PedidosClient pedidosClient;
    private final NotificacionesGateway notificaciones;
    private final AutorizadorSimulado autorizador;
    private final PagoMapper mapper;
    private final Consola consola;
    private final TransactionTemplate transaccion;

    public PagoService(PagoRepository pagoRepository,
                       MedioPagoRepository medioPagoRepository,
                       HistorialEstadoPagoRepository historialRepository,
                       UsuariosClient usuariosClient,
                       PedidosClient pedidosClient,
                       NotificacionesGateway notificaciones,
                       AutorizadorSimulado autorizador,
                       PagoMapper mapper,
                       Consola consola,
                       PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.historialRepository = historialRepository;
        this.usuariosClient = usuariosClient;
        this.pedidosClient = pedidosClient;
        this.notificaciones = notificaciones;
        this.autorizador = autorizador;
        this.mapper = mapper;
        this.consola = consola;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------ SRV-01 (CU-01 / CU-09)

    // Inicia el pago y, si autorizacion.automatica=true, lo resuelve en el momento con el sorteo simulado
    // (APROBADO, RECHAZADO por fondos insuficientes, NO_AUTORIZADO o ERROR).
    public ResultadoOperacion<PagoResponse> iniciarPago(CrearPagoRequest dto) {
        return iniciar(dto, autorizador.isAutomatica());
    }

    // Deja el pago PENDIENTE sin sortear: lo resuelve despues approve / reject (datos de demo y modo manual).
    public ResultadoOperacion<PagoResponse> iniciarPagoPendiente(CrearPagoRequest dto) {
        return iniciar(dto, false);
    }

    // Sin transaccion propia a proposito: las consultas a Usuarios y Pedidos (HTTP) se hacen ANTES de abrir la
    // transaccion, y el aviso a Pedidos y la notificacion DESPUES del commit. La base solo queda tomada para
    // el insert y para el cambio de estado.
    private ResultadoOperacion<PagoResponse> iniciar(CrearPagoRequest dto, boolean autorizar) {

        // @Valid ya garantizo monto > 0 y a lo sumo 2 decimales; se normaliza la escala para comparar y persistir.
        BigDecimal monto = dto.getMonto().setScale(2);

        consola.titulo("INICIAR PAGO");
        consola.paso("SOLICITUD", "Llego el pedido de pago: pedido " + dto.getCodigoPedido() + " por " + monto + " "
                + dto.getMoneda().toUpperCase());
        consola.paso("SOLICITUD", "Usuario " + dto.getCodigoUsuario() + " | clave de control " + dto.getClaveControl());

        try {
            // RN-01: misma clave -> pago existente (mismos datos) o 409 (datos distintos).
            PagoResponse existente = buscarPorClave(dto, monto);
            if (Objects.nonNull(existente)) {
                return repetido(existente);
            }

            MedioPago medioPago = validar(dto, monto);

            PagoResponse creado;
            try {
                creado = transaccion.execute(status -> registrarPago(dto, monto, medioPago));
            } catch (DataIntegrityViolationException e) {
                // Dos solicitudes simultaneas con la misma clave chocaron con el UNIQUE de clave_control:
                // la perdedora devuelve el pago que creo la ganadora (CU-09 4b).
                PagoResponse ganador = buscarPorClave(dto, monto);
                if (Objects.isNull(ganador)) {
                    throw e;
                }
                return repetido(ganador);
            }
            consola.ok("REGISTRO", "Pago " + creado.getCodigoPago() + " creado en estado PENDIENTE");

            if (!autorizar) {
                consola.resultado(true, "PENDIENTE (se resuelve con approve / reject)");
                return new ResultadoOperacion<>(creado, true);
            }

            AutorizadorSimulado.Sorteo sorteo = autorizador.sortear();
            consola.paso("SORTEO", "Salio el " + sorteo.numero() + " de " + sorteo.total() + " ("
                    + autorizador.pesos() + ") -> " + sorteo.resultado().getDescripcion());

            PagoResponse resuelto = transaccion.execute(status ->
                    cambiarEstado(creado.getCodigoPago(), sorteo.resultado().getEstado(), sorteo.motivo())).pago();
            despuesDeResolver(resuelto);
            return new ResultadoOperacion<>(resuelto, true);

        } catch (RuntimeException e) {
            throw fallo(e);
        }
    }

    private ResultadoOperacion<PagoResponse> repetido(PagoResponse existente) {
        consola.ok("IDEMPOTENCIA", "La clave de control ya se uso con los mismos datos: se devuelve el pago "
                + existente.getCodigoPago());
        consola.resultado(true, existente.getEstado() + " (pago existente, no se crea otro)");
        return new ResultadoOperacion<>(existente, false);
    }

    // Transaccion corta de lectura. Devuelve null si la clave no se uso; 409 si se uso con otros datos.
    private PagoResponse buscarPorClave(CrearPagoRequest dto, BigDecimal monto) {
        return transaccion.execute(status -> {
            Pago existente = pagoRepository.findByClaveControl(dto.getClaveControl());
            if (Objects.isNull(existente)) {
                return null;
            }
            boolean mismosDatos = existente.getCodigoPedido().equals(dto.getCodigoPedido())
                    && existente.getCodigoUsuario().equals(dto.getCodigoUsuario())
                    && existente.getMedioPago().getCodigoMedioPago().equals(dto.getCodigoMedioPago())
                    && existente.getMonto().compareTo(monto) == 0;
            if (!mismosDatos) {
                throw new PagoException(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                        "La clave de control ya fue usada con datos distintos: " + dto.getClaveControl());
            }
            return mapper.toResponse(existente);
        });
    }

    // Validaciones contra otros modulos y contra los medios de pago. Corre sin transaccion abierta.
    private MedioPago validar(CrearPagoRequest dto, BigDecimal monto) {

        UsuarioExternoDto usuario = usuariosClient.buscarUsuario(dto.getCodigoUsuario());
        if (Objects.isNull(usuario) || !usuario.isActivo()) {
            throw new PagoException(ErrorCode.PAYMENT_USER_INVALID,
                    "El usuario no existe o esta inactivo: " + dto.getCodigoUsuario());
        }
        consola.ok("VALIDACION", "Usuario activo: " + usuario.getNombre() + " (consultado a Usuarios)");

        MedioPago medioPago = medioPagoRepository.findByCodigoMedioPago(dto.getCodigoMedioPago());
        if (Objects.isNull(medioPago) || !medioPago.getActivo()
                || !medioPago.getCodigoUsuario().equals(dto.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "El medio de pago no existe, esta inactivo o pertenece a otro usuario");
        }
        consola.ok("VALIDACION", "Medio de pago activo y del usuario: " + medioPago.getTipo() + " "
                + medioPago.getDetalleEnmascarado());

        PedidoExternoDto pedido = pedidosClient.buscarPedido(dto.getCodigoPedido(), dto.getCodigoUsuario());
        if (Objects.isNull(pedido) || !"BORRADOR".equals(pedido.getEstado())) {
            throw new PagoException(ErrorCode.ORDER_NOT_PAYABLE,
                    "El pedido no existe o no esta pendiente de pago: " + dto.getCodigoPedido());
        }
        consola.ok("VALIDACION", "Pedido pendiente de pago (BORRADOR), total " + pedido.getTotal()
                + " (consultado a Pedidos)");

        // RN-02: el importe debe coincidir exactamente con el total del pedido.
        if (pedido.getTotal().compareTo(monto) != 0) {
            throw new PagoException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "El monto " + monto + " no coincide con el total del pedido " + pedido.getTotal());
        }
        consola.ok("VALIDACION", "El monto coincide con el total del pedido");
        return medioPago;
    }

    private PagoResponse registrarPago(CrearPagoRequest dto, BigDecimal monto, MedioPago medioPago) {
        Pago pago = new Pago(dto.getCodigoPedido(), dto.getCodigoUsuario(), monto,
                dto.getMoneda().toUpperCase(), dto.getClaveControl(), medioPago);
        HistorialEstadoPago alta = new HistorialEstadoPago(null, EstadoPago.PENDIENTE, "Pago iniciado");
        pago.agregarHistorial(alta);

        pagoRepository.save(pago);
        historialRepository.save(alta);
        return mapper.toResponse(pago);
    }

    // ------------------------------------------------------------ SRV-02 (CU-02)

    public PagoResponse aprobarPago(UUID codigoPago) {
        return resolverManual("APROBAR PAGO", codigoPago, EstadoPago.APROBADO, "Autorizacion simulada aprobada");
    }

    // ------------------------------------------------------------ SRV-03 (CU-03)

    public PagoResponse rechazarPago(UUID codigoPago, RechazarPagoRequest dto) {
        String motivo = (Objects.isNull(dto) || Objects.isNull(dto.getMotivo()) || dto.getMotivo().isBlank())
                ? "Rechazo simulado"
                : dto.getMotivo().trim();
        return resolverManual("RECHAZAR PAGO", codigoPago, EstadoPago.RECHAZADO, motivo);
    }

    private PagoResponse resolverManual(String operacion, UUID codigoPago, EstadoPago destino, String motivo) {
        consola.titulo(operacion);
        consola.paso("SOLICITUD", "Pago " + codigoPago + " -> " + destino);
        try {
            Cambio cambio = transaccion.execute(status -> cambiarEstado(codigoPago, destino, motivo));

            // RN-10: repetir la operacion no genera efectos (ni aviso ni notificacion).
            if (!cambio.cambio()) {
                consola.ok("IDEMPOTENCIA", "El pago ya estaba " + destino + ": no se repite la operacion");
                consola.resultado(true, destino + " (sin cambios)");
                return cambio.pago();
            }
            despuesDeResolver(cambio.pago());
            return cambio.pago();

        } catch (RuntimeException e) {
            throw fallo(e);
        }
    }

    // Dentro de la transaccion: valida la transicion (RN-04), cambia el estado y deja el historial (RN-09).
    private Cambio cambiarEstado(UUID codigoPago, EstadoPago destino, String motivo) {
        Pago pago = buscarEntidad(codigoPago);
        if (pago.getEstado() == destino) {
            return new Cambio(mapper.toResponse(pago), false);
        }
        EstadoPago anterior = pago.getEstado();
        pago.resolver(destino, destino == EstadoPago.APROBADO ? null : motivo);
        registrarHistorial(pago, anterior, motivo);
        pagoRepository.saveAndFlush(pago);
        return new Cambio(mapper.toResponse(pago), true);
    }

    // Ya con el commit hecho: se avisa a Pedidos y se notifica al usuario. Si alguno falla, el pago no cambia.
    private void despuesDeResolver(PagoResponse pago) {
        boolean aprobado = EstadoPago.APROBADO.name().equals(pago.getEstado());
        String detalle = aprobado ? "APROBADO" : pago.getEstado() + " - " + pago.getMotivo();

        if (aprobado) {
            consola.ok("AUTORIZACION", "Pago APROBADO");
        } else {
            consola.fallo("AUTORIZACION", "Pago " + detalle);
        }

        avisarAPedidos(pago);

        if (aprobado) {
            notificaciones.comprobantePago(pago);
        } else {
            notificaciones.avisoRechazo(pago, pago.getMotivo());
        }
        consola.resultado(aprobado, detalle);
    }

    private void avisarAPedidos(PagoResponse pago) {
        try {
            if (pedidosClient.avisarResultadoPago(pago)) {
                consola.ok("PEDIDOS", "Aviso enviado a Pedidos: el pago del pedido " + pago.getCodigoPedido()
                        + " quedo " + pago.getEstado());
            } else {
                consola.paso("PEDIDOS", "Sin endpoint de aviso configurado: Pedidos recibe el resultado en la respuesta");
            }
        } catch (RuntimeException e) {
            consola.fallo("PEDIDOS", "No se pudo avisar a Pedidos (el pago se mantiene)");
            log.error("No se pudo avisar a Pedidos el resultado del pago {}: {}", pago.getCodigoPago(), e.getMessage());
        }
    }

    // Cierra el bloque de consola cuando la operacion termina con error y devuelve la misma excepcion.
    private RuntimeException fallo(RuntimeException e) {
        String detalle = e instanceof PagoException pe ? pe.getCode() + ": " + pe.getMessage()
                : e.getClass().getSimpleName();
        consola.fallo("ERROR", detalle);
        consola.resultado(false, e instanceof PagoException pe
                ? "No se realizo la operacion (" + pe.getCode() + ")" : "No se realizo la operacion");
        return e;
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
