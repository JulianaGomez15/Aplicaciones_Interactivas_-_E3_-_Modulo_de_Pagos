package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.MedioPagoRequest;
import ar.edu.uade.api.pagos.dto.MedioPagoResponse;
import ar.edu.uade.api.pagos.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Medios de pago", description = "Medios de pago simulados: solo se guarda un detalle enmascarado")
@SecurityRequirement(name = "bearerAuth")
public interface IMedioPagoController {

    @Operation(operationId = "API-07_registrarMedioPago", summary = "Registrar medio de pago (CU-08)",
            description = """
                    Registra un medio de pago simulado para el **usuario del JWT** (el cuerpo no lleva codigo de \
                    usuario). Se guardan solo los ultimos 4 digitos (RN-08); el numero completo se descarta.

                    Tipos: TARJETA_CREDITO, TARJETA_DEBITO (13 a 19 digitos), BILLETERA_VIRTUAL, TRANSFERENCIA.

                    **Rol:** CLIENTE.""",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Medio de pago creado y activo",
                            content = @Content(schema = @Schema(implementation = MedioPagoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "PAYMENT_METHOD_INVALID (tipo no soportado o "
                            + "numero invalido) o VALIDATION_ERROR"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el rol no es CLIENTE")
            })
    ResponseEntity<MedioPagoResponse> registrar(MedioPagoRequest dto, JwtPrincipal principal);
}
