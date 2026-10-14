package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.services.PagoServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Pagos", description = "Iniciar, autorizar, rechazar, consultar y reembolsar pagos (simulados)")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoServices pagoServices;

    public PagoController(PagoServices pagoServices) {
        this.pagoServices = pagoServices;
    }

    @Operation(summary = "CU-01/CU-09 Iniciar pago",
            description = "201 si crea el pago; 200 con el pago original si la clave de control ya se uso con los "
                    + "mismos datos; 409 DUPLICATE_PAYMENT_REQUEST si se uso con datos distintos; "
                    + "422 PAYMENT_AMOUNT_MISMATCH si el monto no coincide con el pedido. Roles: ADMIN, PEDIDOS.")
    @PostMapping
    public ResponseEntity<PagoDto> iniciar(@RequestBody IniciarPagoRequestDto dto) {
        IniciarPagoResultadoDto resultado = pagoServices.iniciarPago(dto);
        HttpStatus status = resultado.isCreado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resultado.getPago());
    }

    @Operation(summary = "CU-02 Autorizar pago", description = "PENDIENTE -> APROBADO. Roles: ADMIN, PEDIDOS.")
    @PostMapping("/{codigoPago}/approve")
    public ResponseEntity<PagoDto> aprobar(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoServices.aprobarPago(codigoPago));
    }

    @Operation(summary = "CU-03 Rechazar pago", description = "PENDIENTE -> RECHAZADO. Cuerpo opcional con el motivo. "
            + "Roles: ADMIN, PEDIDOS.")
    @PostMapping("/{codigoPago}/reject")
    public ResponseEntity<PagoDto> rechazar(@PathVariable UUID codigoPago,
                                            @RequestBody(required = false) RechazarPagoRequestDto dto) {
        return ResponseEntity.ok(pagoServices.rechazarPago(codigoPago, dto));
    }

    @Operation(summary = "CU-05 Reembolsar pago", description = "Reembolso total o parcial de un pago APROBADO. "
            + "Roles: ADMIN, PEDIDOS.")
    @PostMapping("/{codigoPago}/refund")
    public ResponseEntity<ReembolsoDto> reembolsar(@PathVariable UUID codigoPago,
                                                   @RequestBody ReembolsoRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoServices.reembolsarPago(codigoPago, dto));
    }

    @Operation(summary = "CU-06 Listar pagos",
            description = "Paginado y filtrable (estado, codigoUsuario, desde, hasta; page, size, sort). "
                    + "Roles: ADMIN, ANALISTA.")
    @GetMapping
    public ResponseEntity<PaginaDto<PagoDto>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID codigoUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(pagoServices.listarPagos(estado, codigoUsuario, desde, hasta, pageable));
    }

    @Operation(summary = "CU-04 Consultar pago", description = "Por codigo publico. Cualquier usuario autenticado.")
    @GetMapping("/{codigoPago}")
    public ResponseEntity<PagoDto> consultar(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoServices.consultarPago(codigoPago));
    }

    @Operation(summary = "CU-07 Pagos de un pedido", description = "Roles: ADMIN, ANALISTA. "
            + "Se expone bajo /order/{codigoPedido} para no chocar con /{codigoPago}.")
    @GetMapping("/order/{codigoPedido}")
    public ResponseEntity<List<PagoDto>> porPedido(@PathVariable UUID codigoPedido) {
        return ResponseEntity.ok(pagoServices.consultarPagosPorPedido(codigoPedido));
    }

    @Operation(summary = "CU-10 Historial de estados", description = "Roles: ADMIN, ANALISTA.")
    @GetMapping("/{codigoPago}/history")
    public ResponseEntity<List<HistorialEstadoPagoDto>> historial(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoServices.consultarHistorial(codigoPago));
    }

    @Operation(summary = "Reembolsos de un pago", description = "Roles: ADMIN, ANALISTA, PEDIDOS.")
    @GetMapping("/{codigoPago}/refunds")
    public ResponseEntity<List<ReembolsoDto>> reembolsos(@PathVariable UUID codigoPago) {
        return ResponseEntity.ok(pagoServices.listarReembolsos(codigoPago));
    }
}
