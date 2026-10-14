package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.MedioPagoDto;
import ar.edu.uade.api.pagos.dto.MedioPagoRequestDto;
import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.UsuariosClient;
import ar.edu.uade.api.pagos.model.MedioPago;
import ar.edu.uade.api.pagos.model.TipoMedioPago;
import ar.edu.uade.api.pagos.repository.MedioPagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MedioPagoServices {

    private final MedioPagoRepository medioPagoRepository;
    private final UsuariosClient usuariosClient;

    public MedioPagoServices(MedioPagoRepository medioPagoRepository, UsuariosClient usuariosClient) {
        this.medioPagoRepository = medioPagoRepository;
        this.usuariosClient = usuariosClient;
    }

    // CU-08
    @Transactional
    public MedioPagoDto crearMedioPago(MedioPagoRequestDto dto) {

        if (Objects.isNull(dto) || Objects.isNull(dto.getCodigoUsuario())) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de usuario es obligatorio");
        }

        UsuarioExternoDto usuario = usuariosClient.buscarUsuario(dto.getCodigoUsuario());
        if (Objects.isNull(usuario) || !usuario.isActivo()) {
            throw new PagoException(ErrorCode.USER_INVALID,
                    "El usuario no existe o esta inactivo: " + dto.getCodigoUsuario());
        }

        MedioPago medioPago = validar(dto);
        medioPagoRepository.save(medioPago);
        return toDto(medioPago);
    }

    @Transactional
    public List<MedioPagoDto> listarPorUsuario(UUID codigoUsuario) {
        if (Objects.isNull(codigoUsuario)) {
            throw new PagoException(ErrorCode.BAD_REQUEST, "El codigo de usuario es obligatorio");
        }
        return medioPagoRepository.findByCodigoUsuarioOrderByFechaCreacionDescIdDesc(codigoUsuario)
                .stream().map(this::toDto).toList();
    }

    private MedioPago validar(MedioPagoRequestDto dto) {

        TipoMedioPago tipo = parsearTipo(dto.getTipo());

        String detalle = dto.getDetalle();
        if (Objects.isNull(detalle) || detalle.isBlank()) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID, "El detalle del medio de pago es obligatorio");
        }

        return new MedioPago(dto.getCodigoUsuario(), tipo, enmascarar(tipo, detalle));
    }

    private TipoMedioPago parsearTipo(String tipo) {
        if (Objects.isNull(tipo) || tipo.isBlank()) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID, "El tipo de medio de pago es obligatorio");
        }
        try {
            return TipoMedioPago.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "Tipo de medio de pago no soportado: " + tipo);
        }
    }

    // RN-08: nunca se persiste el dato completo, solo los ultimos 4 caracteres.
    private String enmascarar(TipoMedioPago tipo, String detalle) {
        String limpio = detalle.replaceAll("[\\s-]", "");

        if (tipo.esTarjeta()) {
            if (!limpio.matches("\\d{13,19}")) {
                throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                        "El numero de tarjeta debe tener entre 13 y 19 digitos");
            }
            return "**** **** **** " + limpio.substring(limpio.length() - 4);
        }

        if (limpio.length() < 4) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "El detalle del medio de pago debe tener al menos 4 caracteres");
        }
        return "****" + limpio.substring(limpio.length() - 4);
    }

    private MedioPagoDto toDto(MedioPago m) {
        return new MedioPagoDto(m.getCodigo(), m.getCodigoUsuario(), m.getTipo().name(),
                m.getDetalleEnmascarado(), m.getActivo(), m.getFechaCreacion());
    }
}
