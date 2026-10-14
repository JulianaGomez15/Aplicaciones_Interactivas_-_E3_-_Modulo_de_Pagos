package ar.edu.uade.api.pagos;

import ar.edu.uade.api.pagos.integration.PedidosClientSimulado;
import ar.edu.uade.api.pagos.integration.UsuariosClientSimulado;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PagosApiTest {

    private static final String PAGOS = "/api/v1/payments";

    @Autowired
    MockMvc mvc;

    // ------------------------------------------------------------------ helpers

    private String login(String user, String pass) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions send(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(req);
    }

    private String crearMedioPago(String clienteToken, UUID usuario, String numero) throws Exception {
        String body = send(post("/api/v1/payment-methods"), clienteToken,
                "{\"codigoUsuario\":\"" + usuario + "\",\"tipo\":\"TARJETA_DEBITO\",\"detalle\":\"" + numero + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalleEnmascarado", is("**** **** **** " + numero.substring(numero.length() - 4))))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.codigo");
    }

    private String iniciarJson(UUID pedido, UUID usuario, String medio, String monto, String clave) {
        return "{\"codigoPedido\":\"" + pedido + "\",\"codigoUsuario\":\"" + usuario + "\",\"codigoMedioPago\":\""
                + medio + "\",\"monto\":" + monto + ",\"claveControl\":\"" + clave + "\"}";
    }

    // ------------------------------------------------------------------ casos

    @Test
    void cicloCompletoIdempotenciaYReembolsos() throws Exception {
        String cliente = login("cliente", "cliente123");
        String pedidos = login("pedidos", "pedidos123");
        String analista = login("analista", "analista123");
        UUID usuario = UsuariosClientSimulado.USUARIO_ANA;
        UUID pedido = PedidosClientSimulado.PEDIDO_3; // total 32000.00
        String medio = crearMedioPago(cliente, usuario, "5500000000004242");

        // CU-01: crea el pago
        String json = iniciarJson(pedido, usuario, medio, "32000.00", "k-1");
        String creado = send(post(PAGOS), pedidos, json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("PENDIENTE")))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creado, "$.codigo");

        // RN-01: misma clave y mismos datos -> 200 con el mismo pago
        send(post(PAGOS), pedidos, json).andExpect(status().isOk()).andExpect(jsonPath("$.codigo", is(codigo)));

        // CU-09: misma clave con datos distintos -> 409
        send(post(PAGOS), pedidos, iniciarJson(pedido, usuario, medio, "31999.00", "k-1"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("DUPLICATE_PAYMENT_REQUEST")));

        // RN-02: monto que no coincide con el pedido -> 422
        send(post(PAGOS), pedidos, iniciarJson(pedido, usuario, medio, "10.00", "k-2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PAYMENT_AMOUNT_MISMATCH")));

        // CU-02: aprobar (y repetir no cambia nada, RN-10)
        send(post(PAGOS + "/" + codigo + "/approve"), pedidos, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado", is("APROBADO")));
        send(post(PAGOS + "/" + codigo + "/approve"), pedidos, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado", is("APROBADO")));

        // RN-04: APROBADO -> RECHAZADO no es una transicion valida
        send(post(PAGOS + "/" + codigo + "/reject"), pedidos, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("PAYMENT_INVALID_STATE")));

        // CU-05: reembolso parcial deja el pago APROBADO
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, "{\"monto\":12000.00,\"motivo\":\"parcial\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago", is("APROBADO")))
                .andExpect(jsonPath("$.montoDisponible", is(20000.00)));

        // RN-07: no se puede superar lo aprobado
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, "{\"monto\":20000.01,\"motivo\":\"de mas\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("REFUND_EXCEEDS_AMOUNT")));

        // Reembolso del saldo -> pago REEMBOLSADO
        String reembolso = send(post(PAGOS + "/" + codigo + "/refund"), pedidos,
                "{\"monto\":20000.00,\"motivo\":\"resto\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estadoPago", is("REEMBOLSADO")))
                .andReturn().getResponse().getContentAsString();
        String codigoReembolso = JsonPath.read(reembolso, "$.codigo");

        // RN-06: ya no es APROBADO -> no admite mas reembolsos
        send(post(PAGOS + "/" + codigo + "/refund"), pedidos, "{\"monto\":1.00,\"motivo\":\"otra vez\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("PAYMENT_INVALID_STATE")));

        // CU-10: historial = alta, aprobacion, reembolso total (RN-09)
        send(get(PAGOS + "/" + codigo + "/history"), analista, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].estadoNuevo", is("PENDIENTE")))
                .andExpect(jsonPath("$[1].estadoNuevo", is("APROBADO")))
                .andExpect(jsonPath("$[2].estadoAnterior", is("APROBADO")))
                .andExpect(jsonPath("$[2].estadoNuevo", is("REEMBOLSADO")));

        send(get(PAGOS + "/" + codigo + "/refunds"), analista, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        send(get("/api/v1/refunds/" + codigoReembolso), analista, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.monto", is(20000.00)));
        send(get("/api/v1/refunds/" + UUID.randomUUID()), analista, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code", is("REFUND_NOT_FOUND")));

        // CU-07: pagos de un pedido
        send(get(PAGOS + "/order/" + pedido), analista, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void pagoRechazadoNoSeReintentaConLaMismaClave() throws Exception {
        String pedidos = login("pedidos", "pedidos123");
        // Demo: pedido 2 tiene un pago RECHAZADO creado con "demo-clave-002" por Bruno.
        String cliente = login("cliente", "cliente123");
        String medio = crearMedioPago(cliente, UsuariosClientSimulado.USUARIO_BRUNO, "4000000000001111");

        // Con la clave nueva (y el mismo pedido) se puede reintentar
        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_2, UsuariosClientSimulado.USUARIO_BRUNO,
                medio, "8500.50", "reintento-1"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado", is("PENDIENTE")));

        // Repetir la clave del pago rechazado (con el medio original) devuelve el rechazado, sin crear otro
        String lista = send(get(PAGOS + "?estado=RECHAZADO"), login("admin", "admin123"), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        String medioOriginal = JsonPath.read(lista, "$.content[0].medioPago.codigo");
        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_2, UsuariosClientSimulado.USUARIO_BRUNO,
                medioOriginal, "8500.50", "demo-clave-002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado", is("RECHAZADO")));
    }

    @Test
    void validacionesDeEntrada() throws Exception {
        String pedidos = login("pedidos", "pedidos123");
        String cliente = login("cliente", "cliente123");
        String medio = crearMedioPago(cliente, UsuariosClientSimulado.USUARIO_ANA, "4111111111111111");

        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_CANCELADO,
                UsuariosClientSimulado.USUARIO_ANA, medio, "4200.00", "v-1"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("ORDER_NOT_PAYABLE")));

        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_3,
                UsuariosClientSimulado.USUARIO_INACTIVO, medio, "32000.00", "v-2"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("USER_INVALID")));

        // El medio de pago es de Ana, no de Bruno
        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_3,
                UsuariosClientSimulado.USUARIO_BRUNO, medio, "32000.00", "v-3"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("PAYMENT_METHOD_INVALID")));

        send(post(PAGOS), pedidos, "{\"codigoPedido\":\"no-es-uuid\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("BAD_REQUEST")));

        send(post(PAGOS), pedidos, iniciarJson(PedidosClientSimulado.PEDIDO_3,
                UsuariosClientSimulado.USUARIO_ANA, medio, "-5", "v-4"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("BAD_REQUEST")));

        // CU-08: tipo no soportado
        send(post("/api/v1/payment-methods"), cliente,
                "{\"codigoUsuario\":\"" + UsuariosClientSimulado.USUARIO_ANA + "\",\"tipo\":\"CHEQUE\",\"detalle\":\"1234\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("PAYMENT_METHOD_INVALID")));

        // CU-06: filtros invalidos
        String admin = login("admin", "admin123");
        send(get(PAGOS + "?estado=XXX"), admin, null).andExpect(status().isBadRequest());
        send(get(PAGOS + "?sort=claveControl,asc"), admin, null).andExpect(status().isBadRequest());
        send(get(PAGOS + "?estado=APROBADO&size=1&page=0"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].estado", is("APROBADO")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void seguridadYFormatoDeError() throws Exception {
        send(get(PAGOS), null, null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        String cliente = login("cliente", "cliente123");
        send(post(PAGOS), cliente, "{}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("FORBIDDEN")));
        send(get(PAGOS), cliente, null).andExpect(status().isForbidden());

        // CU-04: 404 con codigo propio y correlation id propagado
        UUID inexistente = UUID.randomUUID();
        mvc.perform(get(PAGOS + "/" + inexistente).header("Authorization", "Bearer " + cliente)
                        .header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", "corr-123"))
                .andExpect(jsonPath("$.code", is("PAYMENT_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", is("corr-123")))
                .andExpect(jsonPath("$.path", is(PAGOS + "/" + inexistente)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }
}
