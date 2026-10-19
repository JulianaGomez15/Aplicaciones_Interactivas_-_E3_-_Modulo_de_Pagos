package ar.edu.uade.api.pagos.controller;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Contrato OpenAPI de la API de pagos. Los codigos HTTP de cada operacion salen de la tabla "APIs" del TPO.
@Tag(name = "Pagos", description = "Iniciar, autorizar, rechazar, consultar, listar y reembolsar pagos simulados")
@SecurityRequirement(name = "bearerAuth")
public interface IPagoController {

    @Operation(operationId = "API-01_iniciarPago", summary = "Iniciar pago (CU-01 / CU-09)",
            description = """
                    Registra un intento de pago PENDIENTE para un pedido pendiente de pago (BORRADOR).

                    **Idempotencia (RN-01):** si la `claveControl` ya existe con los mismos datos \
                    (pedido, usuario, monto y medio de pago) responde **200** con el pago original sin crear otro; \
                    con datos distintos responde **409 DUPLICATE_PAYMENT_REQUEST**. Un pago RECHAZADO no se reintenta \
                    con la misma clave: hace falta una nueva.

                    **Valida contra otros modulos:** usuario activo (INT-02), medio de pago activo y propio, pedido \
                    pendiente de pago y monto igual al total (INT-01). Si Pedidos o Usuarios no responden: 503 y no se \
                    crea el pago.

                    **Rol:** SERVICIO_ESCRITURA (JWT de servicio de Pedidos).""",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pago creado en estado PENDIENTE",
                            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
                    @ApiResponse(responseCode = "200", description = "La clave de control ya se uso con los mismos "
                            + "datos: se devuelve el pago existente",
                            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (campos faltantes o con formato "
                            + "invalido) o PAYMENT_METHOD_INVALID (medio inexistente, inactivo o de otro usuario)"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: falta el JWT o no es valido"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el rol no tiene permiso de escritura"),
                    @ApiResponse(responseCode = "409", description = "DUPLICATE_PAYMENT_REQUEST: la clave de control "
                            + "ya se uso con datos distintos"),
                    @ApiResponse(responseCode = "422", description = "PAYMENT_AMOUNT_MISMATCH, PAYMENT_USER_INVALID u "
                            + "ORDER_NOT_PAYABLE"),
                    @ApiResponse(responseCode = "503", description = "EXTERNAL_SERVICE_UNAVAILABLE: Pedidos o Usuarios "
                            + "no respondieron; no se crea el pago")
            })
    ResponseEntity<PagoResponse> iniciar(CrearPagoRequest dto);

    @Operation(operationId = "API-02_aprobarPago", summary = "Autorizar pago (CU-02)",
            description = """
                    Autorizacion simulada: PENDIENTE -> APROBADO. Registra el cambio en el historial y pide el \
                    comprobante a Notificaciones (si Notificaciones falla, el pago queda aprobado igual).

                    Con la respuesta, Pedidos confirma el pedido (RN-03). Aprobar un pago ya APROBADO devuelve 200 \
                    sin cambios (RN-10); si esta RECHAZADO o REEMBOLSADO, 409.

                    **Rol:** SERVICIO_ESCRITURA.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pago APROBADO (tambien si ya lo estaba)",
                            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
                    @ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND"),
                    @ApiResponse(responseCode = "409", description = "PAYMENT_INVALID_STATE: el pago esta RECHAZADO "
                            + "o REEMBOLSADO")
            })
    ResponseEntity<PagoResponse> aprobar(
            @Parameter(description = "Codigo publico del pago") UUID codigoPago);

    @Operation(operationId = "API-03_rechazarPago", summary = "Rechazar pago (CU-03)",
            description = """
                    Rechazo simulado: PENDIENTE -> RECHAZADO, con el motivo en el historial. Pide el aviso de rechazo \
                    a Notificaciones. El pedido no avanza (RN-03). Rechazar un pago ya RECHAZADO devuelve 200 sin \
                    cambios (RN-10); si esta APROBADO o REEMBOLSADO, 409. Para reintentar hay que crear un pago \
                    nuevo con otra clave de control.

                    **Rol:** SERVICIO_ESCRITURA.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pago RECHAZADO (tambien si ya lo estaba)",
                            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (motivo demasiado largo)"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
                    @ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND"),
                    @ApiResponse(responseCode = "409", description = "PAYMENT_INVALID_STATE: el pago esta APROBADO "
                            + "o REEMBOLSADO")
            })
    ResponseEntity<PagoResponse> rechazar(
            @Parameter(description = "Codigo publico del pago") UUID codigoPago,
            RechazarPagoRequest dto);

    @Operation(operationId = "API-04_consultarPago", summary = "Consultar pago (CU-04)",
            description = """
                    Busca por el codigo publico (nunca por el id interno). Un usuario con rol CLIENTE solo puede \
                    ver sus propios pagos (se compara el `codigoUsuario` del JWT).

                    **Roles:** CLIENTE (solo sus pagos), ADMINISTRADOR, SERVICIO_LECTURA.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pago",
                            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN: rol sin permiso, o pago de otro "
                            + "usuario"),
                    @ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND")
            })
    ResponseEntity<PagoResponse> consultar(
            @Parameter(description = "Codigo publico del pago") UUID codigoPago,
            JwtPrincipal principal);

    @Operation(operationId = "API-05_reembolsarPago", summary = "Reembolsar pago (CU-05)",
            description = """
                    Reembolso total o parcial, simulado, de un pago APROBADO. El monto no puede superar lo \
                    disponible (monto aprobado menos lo ya reembolsado). Un reembolso parcial deja el pago APROBADO; \
                    el que cubre el total lo pasa a REEMBOLSADO y lo registra en el historial.

                    **Idempotencia (RN-10):** repetir la `claveControl` del reembolso devuelve **200** con el \
                    reembolso existente.

                    **Roles:** SERVICIO_ESCRITURA, ADMINISTRADOR.""",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Reembolso registrado",
                            content = @Content(schema = @Schema(implementation = ReembolsoResponse.class))),
                    @ApiResponse(responseCode = "200", description = "La clave de control del reembolso ya se uso: se "
                            + "devuelve el reembolso existente",
                            content = @Content(schema = @Schema(implementation = ReembolsoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
                    @ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND"),
                    @ApiResponse(responseCode = "409", description = "PAYMENT_INVALID_STATE: el pago no esta APROBADO "
                            + "(incluye REEMBOLSADO) / DUPLICATE_PAYMENT_REQUEST si la clave se uso con otros datos"),
                    @ApiResponse(responseCode = "422", description = "REFUND_EXCEEDS_AMOUNT: el monto supera lo "
                            + "disponible")
            })
    ResponseEntity<ReembolsoResponse> reembolsar(
            @Parameter(description = "Codigo publico del pago") UUID codigoPago,
            ReembolsoRequest dto);

    @Operation(operationId = "API-06_listarPagos", summary = "Listar pagos (CU-06 / CU-07)",
            description = """
                    Listado paginado y filtrable para auditoria y metricas. Con `codigoPedido` devuelve todos los \
                    intentos de pago de un pedido (CU-07): si no tiene pagos, 200 con la lista vacia.

                    Orden: `sort=campo,asc|desc` con campo en fechaCreacion, fechaActualizacion, monto o estado \
                    (por defecto fechaCreacion,desc). Tamano maximo de pagina: 100.

                    **Roles:** ADMINISTRADOR, ANALISTA, SERVICIO_LECTURA.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagina de resultados",
                            content = @Content(schema = @Schema(implementation = PaginaPagosResponse.class))),
                    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: filtros, fechas o sort "
                            + "invalidos"),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN")
            })
    ResponseEntity<PaginaPagosResponse> listar(
            @Parameter(description = "PENDIENTE, APROBADO, RECHAZADO o REEMBOLSADO") String estado,
            @Parameter(description = "Codigo del usuario que pago") UUID codigoUsuario,
            @Parameter(description = "Codigo del pedido (CU-07: intentos de pago del pedido)") UUID codigoPedido,
            @Parameter(description = "Fecha de creacion desde (inclusive), formato yyyy-MM-dd",
                    example = "2026-09-01") LocalDate desde,
            @Parameter(description = "Fecha de creacion hasta (inclusive), formato yyyy-MM-dd",
                    example = "2026-09-30") LocalDate hasta,
            @ParameterObject Pageable pageable);

    @Operation(operationId = "API-08_consultarHistorial", summary = "Historial de estados del pago (CU-10)",
            description = """
                    Linea de tiempo completa del pago, ordenada por fecha, incluida el alta (estadoAnterior nulo).

                    **Rol:** ADMINISTRADOR.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Historial del pago",
                            content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = HistorialEstadoResponse.class)))),
                    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
                    @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
                    @ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND")
            })
    ResponseEntity<List<HistorialEstadoResponse>> historial(
            @Parameter(description = "Codigo publico del pago") UUID codigoPago);
}
