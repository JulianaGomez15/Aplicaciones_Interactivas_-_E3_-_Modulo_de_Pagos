package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.security.JwtPrincipal;
import ar.edu.uade.api.pagos.services.PagoService;
import ar.edu.uade.api.pagos.services.ReembolsoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Sin reglas de negocio: valida superficialmente (@Valid), delega en los Services y traduce el resultado a HTTP.
@RestController
@RequestMapping("/api/v1/payments")
public class PagoController implements IPagoController {

    private final PagoService pagoService;
    private final ReembolsoService reembolsoService;

    public PagoController(PagoService pagoService, ReembolsoService reembolsoService) {
        this.pagoService = pagoService;
        this.reembolsoService = reembolsoService;
    }

    @Override
    @PostMapping
    public ResponseEntity<PagoResponse> iniciar(@Valid @RequestBody CrearPagoRequest dto) {
        ResultadoOperacion<PagoResponse> resultado = pagoService.iniciarPago(dto);
        return ResponseEntity.status(resultado.isCreado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.getCuerpo());
    }

    @Override
    @PostMapping("/{codigoPago}/approve")
    public ResponseEntity<PagoResponse> aprobar(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoService.aprobarPago(codigoPago));
    }

    @Override
    @PostMapping("/{codigoPago}/reject")
    public ResponseEntity<PagoResponse> rechazar(@PathVariable UUID codigoPago,
                                                 @Valid @RequestBody(required = false) RechazarPagoRequest dto) {
        return ResponseEntity.ok(pagoService.rechazarPago(codigoPago, dto));
    }

    @Override
    @GetMapping("/{codigoPago}")
    public ResponseEntity<PagoResponse> consultar(@PathVariable UUID codigoPago,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(pagoService.consultarPago(codigoPago, principal.restringirAUsuario()));
    }

    @Override
    @PostMapping("/{codigoPago}/refund")
    public ResponseEntity<ReembolsoResponse> reembolsar(@PathVariable UUID codigoPago,
                                                        @Valid @RequestBody ReembolsoRequest dto) {
        ResultadoOperacion<ReembolsoResponse> resultado = reembolsoService.reembolsarPago(codigoPago, dto);
        return ResponseEntity.status(resultado.isCreado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.getCuerpo());
    }

    @Override
    @GetMapping
    public ResponseEntity<PaginaPagosResponse> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID codigoUsuario,
            @RequestParam(required = false) UUID codigoPedido,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(
                pagoService.listarPagos(estado, codigoUsuario, codigoPedido, desde, hasta, pageable));
    }

    @Override
    @GetMapping("/{codigoPago}/history")
    public ResponseEntity<List<HistorialEstadoResponse>> historial(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoService.consultarHistorial(codigoPago));
    }
}
