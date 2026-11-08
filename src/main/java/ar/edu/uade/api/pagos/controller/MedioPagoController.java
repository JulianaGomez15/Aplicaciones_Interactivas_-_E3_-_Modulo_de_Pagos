package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.MedioPagoRequest;
import ar.edu.uade.api.pagos.dto.MedioPagoResponse;
import ar.edu.uade.api.pagos.security.JwtPrincipal;
import ar.edu.uade.api.pagos.services.MedioPagoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class MedioPagoController implements IMedioPagoController {

    private final MedioPagoService medioPagoService;

    public MedioPagoController(MedioPagoService medioPagoService) {
        this.medioPagoService = medioPagoService;
    }

    @Override
    @PostMapping
    public ResponseEntity<MedioPagoResponse> registrar(@Valid @RequestBody MedioPagoRequest dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medioPagoService.registrarMedioPago(principal.getCodigoUsuario(), dto));
    }

    @Override
    @DeleteMapping("/{codigoMedioPago}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID codigoMedioPago,
                                         @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        medioPagoService.eliminarMedioPago(codigoMedioPago, principal.getCodigoUsuario());
        return ResponseEntity.noContent().build();
    }
}
