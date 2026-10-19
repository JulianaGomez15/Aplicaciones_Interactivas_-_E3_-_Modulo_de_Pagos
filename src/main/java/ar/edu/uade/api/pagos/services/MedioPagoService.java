package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.dto.MedioPagoRequest;
import ar.edu.uade.api.pagos.dto.MedioPagoResponse;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.model.MedioPago;
import ar.edu.uade.api.pagos.model.TipoMedioPago;
import ar.edu.uade.api.pagos.repository.MedioPagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class MedioPagoService {

    private final MedioPagoRepository medioPagoRepository;

    public MedioPagoService(MedioPagoRepository medioPagoRepository) {
        this.medioPagoRepository = medioPagoRepository;
    }

    // SRV-07 (CU-08): el codigo de usuario sale del JWT (lo resuelve el Controller), no del cuerpo.
    @Transactional
    public MedioPagoResponse registrarMedioPago(UUID codigoUsuario, MedioPagoRequest dto) {

        if (Objects.isNull(codigoUsuario)) {
            throw new PagoException(ErrorCode.FORBIDDEN, "El token no identifica a un usuario");
        }

        MedioPago medioPago = validar(codigoUsuario, dto);
        medioPagoRepository.save(medioPago);
        return new MedioPagoResponse(medioPago.getCodigoMedioPago(), medioPago.getTipo().name(),
                medioPago.getDetalleEnmascarado(), medioPago.getActivo());
    }

    private MedioPago validar(UUID codigoUsuario, MedioPagoRequest dto) {

        if (Objects.isNull(dto) || Objects.isNull(dto.getTipo()) || dto.getTipo().isBlank()) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID, "El tipo de medio de pago es obligatorio");
        }

        TipoMedioPago tipo;
        try {
            tipo = TipoMedioPago.valueOf(dto.getTipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "Tipo de medio de pago no soportado: " + dto.getTipo());
        }

        if (Objects.isNull(dto.getNumero()) || dto.getNumero().isBlank()) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID, "El numero del medio de pago es obligatorio");
        }

        return new MedioPago(codigoUsuario, tipo, enmascarar(tipo, dto.getNumero()));
    }

    // RN-08: nunca se persiste el dato completo, solo los ultimos 4 caracteres.
    private String enmascarar(TipoMedioPago tipo, String numero) {
        String limpio = numero.replaceAll("[\\s-]", "");

        if (tipo.esTarjeta()) {
            if (!limpio.matches("\\d{13,19}")) {
                throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                        "El numero de tarjeta debe tener entre 13 y 19 digitos");
            }
            return "**** **** **** " + limpio.substring(limpio.length() - 4);
        }

        if (limpio.length() < 4) {
            throw new PagoException(ErrorCode.PAYMENT_METHOD_INVALID,
                    "El numero del medio de pago debe tener al menos 4 caracteres");
        }
        return "****" + limpio.substring(limpio.length() - 4);
    }
}
