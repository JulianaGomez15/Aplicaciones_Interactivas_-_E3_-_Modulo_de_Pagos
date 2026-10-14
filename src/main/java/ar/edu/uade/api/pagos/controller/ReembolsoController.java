package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.ReembolsoDto;
import ar.edu.uade.api.pagos.services.PagoServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
@Tag(name = "Reembolsos", description = "Consulta de reembolsos por codigo")
@SecurityRequirement(name = "bearerAuth")
public class ReembolsoController {

    private final PagoServices pagoServices;

    public ReembolsoController(PagoServices pagoServices) {
        this.pagoServices = pagoServices;
    }

    @Operation(summary = "Consultar reembolso", description = "404 REFUND_NOT_FOUND si no existe. "
            + "Roles: ADMIN, ANALISTA, PEDIDOS.")
    @GetMapping("/{codigoReembolso}")
    public ResponseEntity<ReembolsoDto> consultar(@PathVariable UUID codigoReembolso) {
        return ResponseEntity.ok(pagoServices.consultarReembolso(codigoReembolso));
    }
}
