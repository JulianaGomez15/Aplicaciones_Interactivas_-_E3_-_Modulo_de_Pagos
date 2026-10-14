package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.NotificacionesClient;
import ar.edu.uade.api.pagos.integration.PedidosClient;
import ar.edu.uade.api.pagos.integration.UsuariosClient;
import ar.edu.uade.api.pagos.model.*;
import ar.edu.uade.api.pagos.repository.HistorialEstadoPagoRepository;
import ar.edu.uade.api.pagos.repository.MedioPagoRepository;
import ar.edu.uade.api.pagos.repository.PagoRepository;
import ar.edu.uade.api.pagos.repository.ReembolsoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PagoServices {

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("fechaCreacion", "fechaActualizacion", "monto", "estado");

    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final HistorialEstadoPagoRepository historialRepository;
    private final UsuariosClient usuariosClient;
    private final PedidosClient pedidosClient;
    private final NotificacionesClient notificacionesClient;

    public PagoServices(PagoRepository pagoRepository,
                        ReembolsoRepository reembolsoRepository,
                        MedioPagoRepository medioPagoRepository,
                        HistorialEstadoPagoRepository historialRepository,
                        UsuariosClient usuariosClient,
                        PedidosClient pedidosClient,
                        NotificacionesClient notificacionesClient) {
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.historialRepository = historialRepository;
        this.usuariosClient = usuariosClient;
        this.pedidosClient = pedidosClient;
        this.notificacionesClient = notificacionesClient;
    }

    // ---------------------------------------------------------------- CU-01 / CU-09

    @Transactional
    public IniciarPagoResultadoDto iniciarPago(IniciarPagoRequestDto dto) {

        validarSolicitud(dto);
        BigDecimal monto = normalizarMonto(dto.getMonto());

        // RN-01 / CU-09: misma clave de control -> se devuelve el pago original (o se rechaza si cambian los datos).
        // Tambien cubre RN-05: un pago RECHAZADO no se reintenta con la misma clave, solo se devuelve.
        Pago existente = pagoRepository.findByClaveControl(dto.getClaveControl());
        if (Objects.nonNull(existente)) {
            if (!mismosDatos(existente, dto, monto)) {
                throw new PagoException(ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                        "La clave de control ya fue usada con datos distintos: " + dto.getClaveControl());
            }
            return new IniciarPagoResultadoDto(toDto(existente), false);
        }

        // Dependencias externas: usuario, medio de pago y pedido deben existir y ser validos.
        UsuarioExternoDto usuario = usuariosClient.buscarUsuario(dto.getCodigoUsuario());
        if (Objects.isNull(usuario) || !usuario.isActivo()) {
            throw new PagoException(ErrorCode.USER_INVALID,
                    "El usuario no existe o esta inactivo: " + dto.getCodigoUsuario());
        }

        MedioPago medioPago = medioPagoRepository.findByCodigo(dto.getCodigoMedioPago());
        if (Objects.isNull(medioPago) || !medioPago.getActivo()
                || !medioPago.getCodigoUsuario().equals(dto.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "El medio de pago no existe, esta inactivo o no pertenece al usuario");
        }

        PedidoExternoDto pedido = pedidosClient.buscarPedido(dto.getCodigoPedido());
        if (Objects.isNull(pedido) || !"CONFIRMADO".equals(pedido.getEstado())) {
            throw new PagoException(ErrorCode.ORDER_NOT_PAYABLE,
                    "El pedido no existe o no esta CONFIRMADO: " + dto.getCodigoPedido());
        }

        // RN-02: el importe debe coincidir exactamente con el total del pedido.
        if (pedido.getTotal().compareTo(monto) != 0) {
            throw new PagoException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "El monto " + monto + " no coincide con el total del pedido " + pedido.getTotal());
        }

        // Un pedido no puede tener dos pagos vivos a la vez (el reintento solo es valido tras un RECHAZADO).
        if (pagoRepository.existsByCodigoPedidoAndEstadoIn(dto.getCodigoPedido(),
                List.of(EstadoPago.PENDIENTE, EstadoPago.APROBADO))) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "El pedido ya tiene un pago PENDIENTE o APROBADO: " + dto.getCodigoPedido());
        }

        Pago pago = new Pago(dto.getCodigoPedido(), dto.getCodigoUsuario(), monto, dto.getClaveControl(), medioPago);
        HistorialEstadoPago alta = new HistorialEstadoPago(null, EstadoPago.PENDIENTE, "Pago iniciado");
        pago.agregarHistorial(alta);

        pagoRepository.save(pago);
        historialRepository.save(alta);
        return new IniciarPagoResultadoDto(toDto(pago), true);
    }

    // ---------------------------------------------------------------- CU-02

    @Transactional
    public PagoDto aprobarPago(UUID codigoPago) {

        Pago pago = buscarEntidad(codigoPago);

        // RN-10: repetir una operacion ya aplicada no genera efectos adicionales.
        if (pago.getEstado() == EstadoPago.APROBADO) {
            return toDto(pago);
        }

        cambiarEstado(pago, EstadoPago.APROBADO, "Autorizacion simulada aprobada");
        pagoRepository.save(pago);

        PagoDto resultado = toDto(pago);
        pedidosClient.notificarPagoAprobado(pago.getCodigoPedido(), pago.getCodigo());
        notificacionesClient.enviarComprobantePago(resultado);
        return resultado;
    }

    // ---------------------------------------------------------------- CU-03

    @Transactional
    public PagoDto rechazarPago(UUID codigoPago, RechazarPagoRequestDto dto) {

        Pago pago = buscarEntidad(codigoPago);

        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            return toDto(pago);
        }

        String motivo = (Objects.isNull(dto) || Objects.isNull(dto.getMotivo()) || dto.getMotivo().isBlank())
                ? "Rechazo simulado"
                : dto.getMotivo().trim();

        cambiarEstado(pago, EstadoPago.RECHAZADO, motivo);
        pagoRepository.save(pago);

        PagoDto resultado = toDto(pago);
        pedidosClient.notificarPagoRechazado(pago.getCodigoPedido(), pago.getCodigo());
        notificacionesClient.enviarAvisoRechazo(resultado, motivo);
        return resultado;
    }

    // ---------------------------------------------------------------- CU-04

    @Transactional
    public PagoDto consultarPago(UUID codigoPago) {
        return toDto(buscarEntidad(codigoPago));
    }

    // ---------------------------------------------------------------- CU-05

    @Transactional
    public ReembolsoDto reembolsarPago(UUID codigoPago, ReembolsoRequestDto dto) {

        Pago pago = buscarEntidad(codigoPago);

        if (Objects.isNull(dto)) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El cuerpo del reembolso es obligatorio");
        }
        BigDecimal monto = normalizarMonto(dto.getMonto());
        if (Objects.isNull(dto.getMotivo()) || dto.getMotivo().isBlank()) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El motivo del reembolso es obligatorio");
        }

        // RN-06: solo se reembolsa un pago APROBADO.
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "Solo se puede reembolsar un pago APROBADO (estado actual: " + pago.getEstado() + ")");
        }

        // RN-07: la suma de reembolsos no puede superar lo aprobado.
        BigDecimal disponible = pago.montoDisponibleParaReembolso();
        if (monto.compareTo(disponible) > 0) {
            throw new PagoException(ErrorCode.REFUND_EXCEEDS_AMOUNT,
                    "El monto " + monto + " supera lo disponible para reembolsar (" + disponible + ")");
        }

        Reembolso reembolso = new Reembolso(monto, dto.getMotivo().trim());
        pago.agregarReembolso(reembolso);
        reembolsoRepository.save(reembolso);

        // Reembolso total (acumulado) -> el pago pasa a REEMBOLSADO.
        if (pago.montoDisponibleParaReembolso().compareTo(BigDecimal.ZERO) == 0) {
            cambiarEstado(pago, EstadoPago.REEMBOLSADO, "Reembolso total: " + reembolso.getMotivo());
        }
        pagoRepository.save(pago);

        ReembolsoDto resultado = toDto(reembolso);
        PagoDto pagoDto = toDto(pago);
        if (pago.getEstado() == EstadoPago.REEMBOLSADO) {
            pedidosClient.notificarPagoReembolsado(pago.getCodigoPedido(), pago.getCodigo());
        }
        notificacionesClient.enviarComprobanteReembolso(pagoDto, resultado);
        return resultado;
    }

    @Transactional
    public List<ReembolsoDto> listarReembolsos(UUID codigoPago) {
        buscarEntidad(codigoPago);
        return reembolsoRepository.findByPagoCodigoOrderByFechaAscIdAsc(codigoPago)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReembolsoDto consultarReembolso(UUID codigoReembolso) {
        Reembolso reembolso = reembolsoRepository.findByCodigo(codigoReembolso);
        if (Objects.isNull(reembolso)) {
            throw new PagoException(ErrorCode.REFUND_NOT_FOUND,
                    "No existe un reembolso con el codigo " + codigoReembolso);
        }
        return toDto(reembolso);
    }

    // ---------------------------------------------------------------- CU-06

    @Transactional
    public PaginaDto<PagoDto> listarPagos(String estado, UUID codigoUsuario, LocalDate desde, LocalDate hasta,
                                          Pageable pageable) {

        EstadoPago estadoFiltro = parsearEstado(estado);
        if (Objects.nonNull(desde) && Objects.nonNull(hasta) && desde.isAfter(hasta)) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "'desde' no puede ser posterior a 'hasta'");
        }
        for (Sort.Order orden : pageable.getSort()) {
            if (!CAMPOS_ORDENABLES.contains(orden.getProperty())) {
                throw new PagoException(ErrorCode.BAD_REQUEST,
                        "No se puede ordenar por '" + orden.getProperty() + "'. Campos validos: " + CAMPOS_ORDENABLES);
            }
        }

        Page<Pago> pagina = pagoRepository.findAll(
                construirFiltro(estadoFiltro, codigoUsuario, desde, hasta),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()));

        List<PagoDto> contenido = pagina.getContent().stream().map(this::toDto).toList();
        return new PaginaDto<>(contenido, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    // ---------------------------------------------------------------- CU-07

    @Transactional
    public List<PagoDto> consultarPagosPorPedido(UUID codigoPedido) {
        List<Pago> pagos = pagoRepository.findByCodigoPedidoOrderByFechaCreacionDesc(codigoPedido);
        if (pagos.isEmpty()) {
            throw new PagoException(ErrorCode.PAYMENT_NOT_FOUND,
                    "No existen pagos para el pedido " + codigoPedido);
        }
        return pagos.stream().map(this::toDto).toList();
    }

    // ---------------------------------------------------------------- CU-10

    @Transactional
    public List<HistorialEstadoPagoDto> consultarHistorial(UUID codigoPago) {
        buscarEntidad(codigoPago);
        return historialRepository.findByPagoCodigoOrderByFechaAscIdAsc(codigoPago).stream()
                .map(h -> new HistorialEstadoPagoDto(
                        Objects.isNull(h.getEstadoAnterior()) ? null : h.getEstadoAnterior().name(),
                        h.getEstadoNuevo().name(), h.getMotivo(), h.getFecha()))
                .toList();
    }

    // ---------------------------------------------------------------- privados

    private Pago buscarEntidad(UUID codigoPago) {
        if (Objects.isNull(codigoPago)) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de pago es obligatorio");
        }
        Pago pago = pagoRepository.findByCodigo(codigoPago);
        if (Objects.isNull(pago)) {
            throw new PagoException(ErrorCode.PAYMENT_NOT_FOUND, "No existe un pago con el codigo " + codigoPago);
        }
        return pago;
    }

    // RN-04 + RN-09: valida la transicion y deja el rastro en el historial. Valida ANTES de mutar.
    private void cambiarEstado(Pago pago, EstadoPago nuevo, String motivo) {
        EstadoPago anterior = pago.getEstado();
        if (!anterior.permiteTransicionA(nuevo)) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "Transicion invalida: " + anterior + " -> " + nuevo);
        }
        HistorialEstadoPago registro = new HistorialEstadoPago(anterior, nuevo, motivo);
        pago.agregarHistorial(registro);
        pago.setEstado(nuevo);
        historialRepository.save(registro);
    }

    private void validarSolicitud(IniciarPagoRequestDto dto) {
        if (Objects.isNull(dto)) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }
        if (Objects.isNull(dto.getCodigoPedido())) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de pedido es obligatorio");
        }
        if (Objects.isNull(dto.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de usuario es obligatorio");
        }
        if (Objects.isNull(dto.getCodigoMedioPago())) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de medio de pago es obligatorio");
        }
        if (Objects.isNull(dto.getClaveControl()) || dto.getClaveControl().isBlank()) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "La clave de control es obligatoria");
        }
        if (dto.getClaveControl().length() > 255) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "La clave de control no puede superar 255 caracteres");
        }
    }

    // Monto > 0 y a lo sumo 2 decimales; se normaliza a escala 2 para comparar y persistir.
    private BigDecimal normalizarMonto(BigDecimal monto) {
        if (Objects.isNull(monto) || monto.signum() <= 0) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El monto debe ser mayor que cero");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El monto admite como maximo 2 decimales");
        }
        return monto.setScale(2);
    }

    private boolean mismosDatos(Pago pago, IniciarPagoRequestDto dto, BigDecimal monto) {
        return pago.getCodigoPedido().equals(dto.getCodigoPedido())
                && pago.getCodigoUsuario().equals(dto.getCodigoUsuario())
                && pago.getMedioPago().getCodigo().equals(dto.getCodigoMedioPago())
                && pago.getMonto().compareTo(monto) == 0;
    }

    private EstadoPago parsearEstado(String estado) {
        if (Objects.isNull(estado) || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPago.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "Estado invalido: " + estado);
        }
    }

    private Specification<Pago> construirFiltro(EstadoPago estado, UUID codigoUsuario, LocalDate desde,
                                                LocalDate hasta) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> condiciones = new ArrayList<>();
            if (Objects.nonNull(estado)) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (Objects.nonNull(codigoUsuario)) {
                condiciones.add(cb.equal(root.get("codigoUsuario"), codigoUsuario));
            }
            if (Objects.nonNull(desde)) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde.atStartOfDay()));
            }
            if (Objects.nonNull(hasta)) {
                condiciones.add(cb.lessThan(root.get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(condiciones.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private PagoDto toDto(Pago p) {
        MedioPago m = p.getMedioPago();
        MedioPagoDto medio = new MedioPagoDto(m.getCodigo(), m.getCodigoUsuario(), m.getTipo().name(),
                m.getDetalleEnmascarado(), m.getActivo(), m.getFechaCreacion());
        return new PagoDto(p.getCodigo(), p.getCodigoPedido(), p.getCodigoUsuario(), p.getMonto(), p.getMoneda(),
                p.getEstado().name(), p.getClaveControl(), medio, p.totalReembolsado(),
                p.getFechaCreacion(), p.getFechaActualizacion());
    }

    private ReembolsoDto toDto(Reembolso r) {
        Pago p = r.getPago();
        return new ReembolsoDto(r.getCodigo(), p.getCodigo(), r.getMonto(), r.getMotivo(), r.getEstado().name(),
                r.getFecha(), p.getEstado().name(), p.montoDisponibleParaReembolso());
    }
}
