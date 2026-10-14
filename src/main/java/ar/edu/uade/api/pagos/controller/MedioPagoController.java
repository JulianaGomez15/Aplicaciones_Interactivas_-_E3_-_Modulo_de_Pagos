package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.MedioPagoDto;
import ar.edu.uade.api.pagos.dto.MedioPagoRequestDto;
import ar.edu.uade.api.pagos.services.MedioPagoServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Medios de pago", description = "CU-08: medios de pago simulados (solo detalle enmascarado)")
@SecurityRequirement(name = "bearerAuth")
public class MedioPagoController {

    private final MedioPagoServices medioPagoServices;

    public MedioPagoController(MedioPagoServices medioPagoServices) {
        this.medioPagoServices = medioPagoServices;
    }

    @Operation(summary = "Registrar medio de pago",
            description = "Tipos: TARJETA_CREDITO, TARJETA_DEBITO, BILLETERA_VIRTUAL, TRANSFERENCIA. "
                    + "400 PAYMENT_METHOD_INVALID si el tipo o el detalle no son validos. Roles: ADMIN, CLIENTE.")
    @PostMapping
    public ResponseEntity<MedioPagoDto> crear(@RequestBody MedioPagoRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medioPagoServices.crearMedioPago(dto));
    }

    @Operation(summary = "Listar medios de pago de un usuario", description = "Roles: ADMIN, CLIENTE.")
    @GetMapping
    public ResponseEntity<List<MedioPagoDto>> listar(@RequestParam UUID codigoUsuario) {
        return ResponseEntity.ok(medioPagoServices.listarPorUsuario(codigoUsuario));
    }
}
