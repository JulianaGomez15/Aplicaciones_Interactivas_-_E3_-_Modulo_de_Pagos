package ar.edu.uade.api.pagos;

import ar.edu.uade.api.pagos.dto.CrearPagoRequest;
import ar.edu.uade.api.pagos.dto.MedioPagoRequest;
import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ResultadoOperacion;
import ar.edu.uade.api.pagos.integration.PedidosClientSimulado;
import ar.edu.uade.api.pagos.integration.UsuariosClientSimulado;
import ar.edu.uade.api.pagos.security.CuentasDemo;
import ar.edu.uade.api.pagos.security.JwtService;
import ar.edu.uade.api.pagos.services.MedioPagoService;
import ar.edu.uade.api.pagos.services.PagoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// autorizacion.automatica=false: los pagos quedan PENDIENTE y se resuelven con approve / reject (sin sorteo).
// El sorteo se prueba en AutorizacionSimuladaTest.
@SpringBootTest(properties = {"integraciones.modo=simulado", "autorizacion.automatica=false"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PagosApiTest {

    private static final String PAGOS = "/api/v1/payments";
    private static final UUID ANA = UsuariosClientSimulado.USUARIO_ANA;
    // Un cliente distinto del usuario de demo, solo para probar que nadie opera sobre datos ajenos.
    private static final UUID OTRO_USUARIO = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Autowired
    MockMvc mvc;
    @Autowired
    PagoService pagoService;
    @Autowired
    MedioPagoService medioPagoService;
    @Autowired
    JwtService jwtService;

    // ------------------------------------------------------------------ helpers

    // La app tiene una sola cuenta de demo con todos los roles. Para probar la autorizacion por rol se emiten
    // JWT con un rol cada uno, como los que emitiria el Servicio de Usuarios.
    private String token(String nombre, UUID codigoUsuario, String... roles) {
        return jwtService.generarToken(nombre, List.of(roles), codigoUsuario);
    }

    private String pedidos() { return token("api-pedidos", null, "SERVICIO_ESCRITURA", "SERVICIO_LECTURA"); }
    private String analitica() { return token("api-analitica", null, "SERVICIO_LECTURA"); }
    private String admin() { return token("admin", null, "ADMINISTRADOR"); }
    private String analista() { return token("analista", null, "ANALISTA"); }
    private String clienteAna() { return token("ana", ANA, "CLIENTE"); }
    private String otroCliente() { return token("otro", OTRO_USUARIO, "CLIENTE"); }

    private ResultActions send(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(req);
    }

    // El cliente se registra un medio de pago (el usuario sale de su JWT).
    private String registrarMedio(String clienteToken, String numero) throws Exception {
        String body = send(post("/api/v1/payment-methods"), clienteToken,
                "{\"tipo\":\"TARJETA_DEBITO\",\"numero\":\"" + numero + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalleEnmascarado", is("**** **** **** " + numero.substring(numero.length() - 4))))
                .andExpect(jsonPath("$.activo", is(true)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.codigoMedioPago");
    }

    private String crearPagoJson(UUID pedido, UUID usuario, String medio, String monto, String clave) {
        return "{\"codigoPedido\":\"" + pedido + "\",\"codigoUsuario\":\"" + usuario + "\",\"codigoMedioPago\":\""
                + medio + "\",\"monto\":" + monto + ",\"moneda\":\"ARS\",\"claveControl\":\"" + clave + "\"}";
    }

    private String reembolsoJson(String monto, String clave) {
        return "{\"monto\":" + monto + ",\"motivo\":\"motivo\",\"claveControl\":\"" + clave + "\"}";
    }

    // ------------------------------------------------------------------ casos

    @Test
    void eliminarMedioPagoDevuelve204YLoDesactiva() throws Exception {
        String ana = clienteAna();
        String medio = registrarMedio(ana, "4111111111111111");
        String ruta = "/api/v1/payment-methods/" + medio;

        send(delete(ruta), otroCliente(), null).andExpect(status().isForbidden());
        send(delete(ruta), ana, null).andExpect(status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
        send(delete(ruta), ana, null).andExpect(status().isNoContent());
        send(delete("/api/v1/payment-methods/" + UUID.randomUUID()), ana, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PAYMENT_METHOD_NOT_FOUND")));
        send(post(PAGOS), pedidos(), crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA,
                medio, "32000.00", "medio-eliminado"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PAYMENT_METHOD_INVALID")));
    }

    @Test
    void cicloCompletoIdempotenciaYReembolsos() throws Exception {
        String pedidos = pedidos();
        UUID pedido = PedidosClientSimulado.PEDIDO_3; // 32000.00
        String medio = registrarMedio(clienteAna(), "5500000000004242");

        // CU-01: crea el pago
        String json = crearPagoJson(pedido, ANA, medio, "32000.00", "k-1");
        String creado = send(post(PAGOS), pedidos, json)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/payments/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")))
                .andExpect(jsonPath("$.motivo").doesNotExist())
                .andExpect(jsonPath("$.medioPago.tipo", is("TARJETA_DEBITO")))
                .andExpect(jsonPath("$.medioPago.detalleEnmascarado", is("**** **** **** 4242")))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creado, "$.codigoPago");

        // RN-01: misma clave y mismos datos -> 200 con el mismo pago
        send(post(PAGOS), pedidos, json).andExpect(status().isOk()).andExpect(jsonPath("$.codigoPago", is(codigo)));

        // CU-09: misma clave con datos distintos -> 409
        send(post(PAGOS), pedidos, crearPagoJson(pedido, ANA, medio, "31999.00", "k-1"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("DUPLICATE_PAYMENT_REQUEST")));

        // RN-02: monto distinto al del pedido -> 422
        send(post(PAGOS), pedidos, crearPagoJson(pedido, ANA, medio, "10.00", "k-2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PAYMENT_AMOUNT_MISMATCH")));

        // CU-02: aprobar (repetir no cambia nada, RN-10)
        send(patch(PAGOS + "/" + codigo + "/approve"), pedidos, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado", is("APROBADO")));
        send(patch(PAGOS + "/" + codigo + "/approve"), pedidos, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado", is("APROBADO")));

        // RN-04: APROBADO -> RECHAZADO no es valido
        send(patch(PAGOS + "/" + codigo + "/reject"), pedidos, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("PAYMENT_INVALID_STATE")));

        // CU-05: reembolso parcial deja el pago APROBADO
        String parcial = send(post(PAGOS + "/" + codigo + "/refund"), pedidos, reembolsoJson("12000.00", "r-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("APROBADO")))
                .andExpect(jsonPath("$.codigoPago", is(codigo)))
                .andExpect(jsonPath("$.montoDisponible", is(20000.00)))
                .andExpect(jsonPath("$.fechaSolicitud", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String codigoReembolso = JsonPath.read(parcial, "$.codigoReembolso");

        // RN-10: misma clave de reembolso -> 200 con el reembolso existente
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, reembolsoJson("12000.00", "r-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoReembolso", is(codigoReembolso)))
                .andExpect(jsonPath("$.montoDisponible", is(20000.00)));

        // RN-07: no se puede superar lo aprobado
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, reembolsoJson("20000.01", "r-2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("REFUND_EXCEEDS_AMOUNT")));

        // El resto -> pago REEMBOLSADO (el administrador tambien puede reembolsar)
        send(post(PAGOS + "/" + codigo + "/refund"), admin(), reembolsoJson("20000.00", "r-3"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.montoDisponible", is(0.0)));
        send(get(PAGOS + "/" + codigo), pedidos, null).andExpect(jsonPath("$.estado", is("REEMBOLSADO")));

        // RN-06: ya no es APROBADO
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, reembolsoJson("1.00", "r-4"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("PAYMENT_INVALID_STATE")));

        // CU-10: historial = alta, aprobacion, reembolso total (RN-09), solo para ADMINISTRADOR
        send(get(PAGOS + "/" + codigo + "/history"), admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].estadoNuevo", is("PENDIENTE")))
                .andExpect(jsonPath("$[1].estadoNuevo", is("APROBADO")))
                .andExpect(jsonPath("$[2].estadoAnterior", is("APROBADO")))
                .andExpect(jsonPath("$[2].estadoNuevo", is("REEMBOLSADO")));

        // CU-07: pagos de un pedido, con el listado y el filtro codigoPedido
        send(get(PAGOS + "?codigoPedido=" + pedido), analitica(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigoPago", is(codigo)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void pagoRechazadoSeReintentaConClaveNueva() throws Exception {
        String pedidos = pedidos();
        // Demo: el pedido 2 tiene un pago RECHAZADO con "demo-clave-002".
        String medio = registrarMedio(clienteAna(), "4000000000001111");

        // RN-05: con una clave nueva y el mismo pedido se puede reintentar
        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_2, ANA, medio, "8500.50", "reintento-1"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado", is("PENDIENTE")));

        // El pedido ahora tiene dos intentos (CU-07)
        send(get(PAGOS + "?codigoPedido=" + PedidosClientSimulado.PEDIDO_2), pedidos, null)
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Repetir la clave del pago rechazado (mismo medio original) devuelve ese pago, sin crear otro
        String rechazado = send(get(PAGOS + "?estado=RECHAZADO"), admin(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        assertEquals("RECHAZADO", JsonPath.read(rechazado, "$.content[0].estado"));
    }

    @Test
    void validacionesDeEntradaYServiciosExternos() throws Exception {
        String pedidos = pedidos();
        String medio = registrarMedio(clienteAna(), "4111111111111111");

        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_CANCELADO, ANA, medio, "4200.00", "v-1"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("ORDER_NOT_PAYABLE")));

        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_3,
                UsuariosClientSimulado.USUARIO_INACTIVO, medio, "32000.00", "v-2"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("PAYMENT_USER_INVALID")));

        // Un medio de pago de otro usuario no sirve para pagar
        String ajeno = medioPagoService.registrarMedioPago(OTRO_USUARIO,
                new MedioPagoRequest("TARJETA_CREDITO", "4111111111111111")).getCodigoMedioPago().toString();
        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA, ajeno, "32000.00", "v-3"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("PAYMENT_METHOD_INVALID")));

        // 5c: Pedidos / Usuarios no responden -> 503 y no se crea el pago
        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_SIN_RESPUESTA, ANA, medio, "10.00", "v-4"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("EXTERNAL_SERVICE_UNAVAILABLE")));
        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_3,
                UsuariosClientSimulado.USUARIO_SIN_RESPUESTA, medio, "32000.00", "v-5"))
                .andExpect(status().isServiceUnavailable());
        send(get(PAGOS + "?codigoPedido=" + PedidosClientSimulado.PEDIDO_3), pedidos, null)
                .andExpect(jsonPath("$.content", hasSize(0))); // CU-07 2a: lista vacia, 200

        // @Valid: campos faltantes / formato invalido -> VALIDATION_ERROR
        send(post(PAGOS), pedidos, "{\"codigoPedido\":\"no-es-uuid\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        send(post(PAGOS), pedidos, "{}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("claveControl")));
        send(post(PAGOS), pedidos, crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA, medio, "-5", "v-6"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

        // CU-08: tipo no soportado
        send(post("/api/v1/payment-methods"), clienteAna(), "{\"tipo\":\"CHEQUE\",\"numero\":\"1234\"}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("PAYMENT_METHOD_INVALID")));

        // CU-06: filtros invalidos
        String admin = admin();
        send(get(PAGOS + "?estado=XXX"), admin, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        send(get(PAGOS + "?sort=claveControl,asc"), admin, null).andExpect(status().isBadRequest());
        send(get(PAGOS + "?desde=2026-02-01&hasta=2026-01-01"), admin, null).andExpect(status().isBadRequest());
        send(get(PAGOS + "?desde=ayer"), admin, null).andExpect(status().isBadRequest());
        send(get(PAGOS + "?estado=APROBADO&size=1&page=0"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].estado", is("APROBADO")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void autorizacionPorRol() throws Exception {
        send(get(PAGOS), null, null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        send(get(PAGOS), "token.invalido.xxx", null).andExpect(status().isUnauthorized());

        String pedidos = pedidos();
        String ana = clienteAna();
        String medio = registrarMedio(ana, "4111111111111111");
        String creado = send(post(PAGOS), pedidos,
                crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA, medio, "32000.00", "rol-1"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creado, "$.codigoPago");

        // Solo el servicio de escritura crea / aprueba / rechaza
        String json = crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA, medio, "32000.00", "rol-2");
        send(post(PAGOS), ana, json).andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("FORBIDDEN")));
        send(post(PAGOS), admin(), json).andExpect(status().isForbidden());
        send(post(PAGOS), analitica(), json).andExpect(status().isForbidden());
        send(patch(PAGOS + "/" + codigo + "/approve"), admin(), null).andExpect(status().isForbidden());
        send(patch(PAGOS + "/" + codigo + "/reject"), analitica(), null).andExpect(status().isForbidden());
        send(post(PAGOS + "/" + codigo + "/refund"), analista(), reembolsoJson("1.00", "x")).andExpect(status().isForbidden());

        // Listado: ADMINISTRADOR, ANALISTA y servicio de lectura; no CLIENTE
        send(get(PAGOS), ana, null).andExpect(status().isForbidden());
        send(get(PAGOS), analista(), null).andExpect(status().isOk());
        send(get(PAGOS), analitica(), null).andExpect(status().isOk());

        // Historial: solo ADMINISTRADOR
        send(get(PAGOS + "/" + codigo + "/history"), analista(), null).andExpect(status().isForbidden());
        send(get(PAGOS + "/" + codigo + "/history"), admin(), null).andExpect(status().isOk());

        // CU-04: el CLIENTE ve solo sus pagos
        send(get(PAGOS + "/" + codigo), ana, null).andExpect(status().isOk());
        send(get(PAGOS + "/" + codigo), otroCliente(), null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("FORBIDDEN")));
        send(get(PAGOS + "/" + codigo), admin(), null).andExpect(status().isOk());
        send(get(PAGOS + "/" + codigo), analitica(), null).andExpect(status().isOk());
        send(get(PAGOS + "/" + codigo), analista(), null).andExpect(status().isForbidden());

        // Medios de pago: solo CLIENTE
        String medioJson = "{\"tipo\":\"TARJETA_CREDITO\",\"numero\":\"4111111111111111\"}";
        send(post("/api/v1/payment-methods"), admin(), medioJson).andExpect(status().isForbidden());
        send(post("/api/v1/payment-methods"), pedidos, medioJson).andExpect(status().isForbidden());

        // Endpoints que ya no existen en el contrato
        send(get(PAGOS + "/order/" + PedidosClientSimulado.PEDIDO_3), admin(), null).andExpect(status().is4xxClientError());
        send(get(PAGOS + "/" + codigo + "/refunds"), admin(), null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    // Ruta, metodo o formato equivocados no deben confundirse con un problema de roles ni dar 500.
    @Test
    void rutaMetodoYFormatoEquivocados() throws Exception {
        String pedidos = pedidos();
        String ruta = PAGOS + "/" + UUID.randomUUID() + "/approve";

        // El contrato es PATCH: un POST con token valido responde 405, no 403
        send(post(ruta), pedidos, null)
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code", is("METHOD_NOT_ALLOWED")))
                // El 405 informa que metodo si acepta la ruta
                .andExpect(header().string("Allow", "PATCH"));
        // Sin token sigue siendo 401
        send(post(ruta), null, null).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        // Ruta inexistente con token valido: 404
        send(get("/api/v1/no-existe"), pedidos, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code", is("NOT_FOUND")));

        // Content-Type que no es JSON: 415
        mvc.perform(post(PAGOS).header("Authorization", "Bearer " + pedidos)
                        .contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_MEDIA_TYPE")));

        // Accept que la API no produce: 406 (con el error igual en JSON)
        mvc.perform(get(PAGOS).header("Authorization", "Bearer " + admin()).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code", is("NOT_ACCEPTABLE")));
    }

    @Test
    void formatoDeErrorYCorrelationId() throws Exception {
        String ana = clienteAna();
        UUID inexistente = UUID.randomUUID();
        mvc.perform(get(PAGOS + "/" + inexistente).header("Authorization", "Bearer " + ana)
                        .header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", "corr-123"))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("PAYMENT_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", is("corr-123")))
                .andExpect(jsonPath("$.path", is(PAGOS + "/" + inexistente)))
                // Sin zona horaria, igual que las fechas de los pagos
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")))
                .andExpect(jsonPath("$.message", notNullValue()));

        // Sin header, el modulo genera uno
        mvc.perform(get(PAGOS + "/" + inexistente).header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.correlationId", not(emptyOrNullString())));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    // Un solo usuario de demo alcanza para todo el recorrido: medio de pago, pago, reembolso, listado e historial.
    @Test
    void elUsuarioDemoHaceTodoElRecorrido() throws Exception {
        String loginBody = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(5)))
                .andReturn().getResponse().getContentAsString();
        String demo = JsonPath.read(loginBody, "$.token");
        assertEquals(1, CuentasDemo.CUENTAS.size(), "debe haber una sola cuenta de demo");

        String medio = registrarMedio(demo, "4111111111111111");
        String creado = send(post(PAGOS), demo,
                crearPagoJson(PedidosClientSimulado.PEDIDO_3, ANA, medio, "32000.00", "demo-unico"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creado, "$.codigoPago");

        send(patch(PAGOS + "/" + codigo + "/approve"), demo, null).andExpect(status().isOk());
        send(post(PAGOS + "/" + codigo + "/refund"), demo, reembolsoJson("32000.00", "demo-reembolso"))
                .andExpect(status().isCreated());
        send(get(PAGOS + "/" + codigo), demo, null).andExpect(jsonPath("$.estado", is("REEMBOLSADO")));
        send(get(PAGOS + "?codigoPedido=" + PedidosClientSimulado.PEDIDO_3), demo, null)
                .andExpect(jsonPath("$.content", hasSize(1)));
        send(get(PAGOS + "/" + codigo + "/history"), demo, null).andExpect(jsonPath("$", hasSize(3)));

        // Las cuentas anteriores ya no existen
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // CU-09 4b: dos solicitudes simultaneas con la misma clave -> un solo pago, ambas reciben el mismo.
    @Test
    void solicitudesSimultaneasConLaMismaClaveCreanUnSoloPago() throws Exception {
        UUID medio = UUID.fromString(registrarMedioDirecto());
        CrearPagoRequest dto = new CrearPagoRequest(PedidosClientSimulado.PEDIDO_3, ANA, medio,
                new BigDecimal("32000.00"), "ARS", "carrera-1");

        int hilos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ResultadoOperacion<PagoResponse>>> futuros = new java.util.ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return pagoService.iniciarPago(dto);
            }));
        }
        largada.countDown();

        long creados = 0;
        UUID codigo = null;
        for (Future<ResultadoOperacion<PagoResponse>> f : futuros) {
            ResultadoOperacion<PagoResponse> r = f.get(30, TimeUnit.SECONDS);
            if (r.isCreado()) creados++;
            if (codigo == null) codigo = r.getCuerpo().getCodigoPago();
            assertEquals(codigo, r.getCuerpo().getCodigoPago());
        }
        pool.shutdown();
        assertEquals(1, creados, "solo una solicitud puede crear el pago");
        assertTrue(creados <= 1);
    }

    private String registrarMedioDirecto() {
        return medioPagoService.registrarMedioPago(ANA, new MedioPagoRequest("TARJETA_CREDITO", "4111111111111111"))
                .getCodigoMedioPago().toString();
    }
}
