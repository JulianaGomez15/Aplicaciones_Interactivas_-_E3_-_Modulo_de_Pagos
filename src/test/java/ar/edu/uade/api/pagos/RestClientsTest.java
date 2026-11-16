package ar.edu.uade.api.pagos;

import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.PedidoExternoDto;
import ar.edu.uade.api.pagos.dto.UsuarioExternoDto;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.PedidosClientRest;
import ar.edu.uade.api.pagos.integration.UsuariosClientRest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Prueba los clients REST contra un servidor HTTP falso (MockRestServiceServer):
// no hace falta que Pedidos ni Usuarios esten levantados.
class RestClientsTest {

    private static final UUID PEDIDO = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void pedidoExistenteSeConvierteEnDto() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pedidos");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        PedidosClientRest cliente = new PedidosClientRest(builder.build(), "");

        servidor.expect(requestTo("http://pedidos/api/v1/orders/" + PEDIDO))
                .andExpect(method(HttpMethod.GET))
                // Pedidos exige X-Cliente-Id y devuelve el codigo del pedido en "id" (swagger del ecosistema)
                .andExpect(header("X-Cliente-Id", USUARIO.toString()))
                .andRespond(withSuccess("""
                        {"id":"%s","total":32000.00,"estado":"BORRADOR","campoQueNoUsamos":"x"}
                        """.formatted(PEDIDO), MediaType.APPLICATION_JSON));

        PedidoExternoDto pedido = cliente.buscarPedido(PEDIDO, USUARIO);

        assertEquals(PEDIDO, pedido.getCodigoPedido());
        assertEquals(0, new BigDecimal("32000.00").compareTo(pedido.getTotal()));
        assertEquals("BORRADOR", pedido.getEstado());
        servidor.verify();
    }

    @Test
    void pedidoInexistenteDevuelveNull() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pedidos");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        PedidosClientRest cliente = new PedidosClientRest(builder.build(), "");

        servidor.expect(requestTo("http://pedidos/api/v1/orders/" + PEDIDO))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertNull(cliente.buscarPedido(PEDIDO, USUARIO));
    }

    @Test
    void pedidosCaidoLanza503() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pedidos");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        PedidosClientRest cliente = new PedidosClientRest(builder.build(), "");

        servidor.expect(requestTo("http://pedidos/api/v1/orders/" + PEDIDO))
                .andRespond(withServerError());

        PagoException e = assertThrows(PagoException.class, () -> cliente.buscarPedido(PEDIDO, USUARIO));
        assertEquals(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, e.getCode());
    }

    @Test
    void usuarioInactivoSeConvierteEnDto() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://usuarios");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        UsuariosClientRest cliente = new UsuariosClientRest(builder.build());

        servidor.expect(requestTo("http://usuarios/api/v1/users/" + USUARIO))
                .andRespond(withSuccess("""
                        {"codigoUsuario":"%s","nombre":"Ana Perez","activo":false}
                        """.formatted(USUARIO), MediaType.APPLICATION_JSON));

        UsuarioExternoDto usuario = cliente.buscarUsuario(USUARIO);

        assertEquals("Ana Perez", usuario.getNombre());
        assertFalse(usuario.isActivo());
    }

    // El swagger de Usuarios informa estado: "ACTIVO" (no un booleano "activo") y su id no es un UUID.
    @Test
    void usuarioConEstadoActivoDelSwaggerDeUsuarios() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://usuarios");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        UsuariosClientRest cliente = new UsuariosClientRest(builder.build());

        servidor.expect(requestTo("http://usuarios/api/v1/users/" + USUARIO))
                .andRespond(withSuccess("""
                        {"id":1,"nombre":"Ana","apellido":"Perez","rol":"CLIENTE","estado":"ACTIVO"}
                        """, MediaType.APPLICATION_JSON));

        UsuarioExternoDto usuario = cliente.buscarUsuario(USUARIO);

        assertTrue(usuario.isActivo());
        assertEquals(USUARIO, usuario.getCodigoUsuario());
    }

    @Test
    void avisoAPedidosSoloSiHayRutaConfigurada() {
        PagoResponse pago = new PagoResponse(UUID.randomUUID(), PEDIDO, "APROBADO", null,
                new BigDecimal("32000.00"), "ARS", null, null, null);

        // Sin ruta: no se llama a nadie
        RestClient.Builder sinRuta = RestClient.builder().baseUrl("http://pedidos");
        MockRestServiceServer servidorSinRuta = MockRestServiceServer.bindTo(sinRuta).build();
        assertFalse(new PedidosClientRest(sinRuta.build(), "").avisarResultadoPago(pago));
        servidorSinRuta.verify();

        // Con ruta: POST con el estado del pago
        RestClient.Builder conRuta = RestClient.builder().baseUrl("http://pedidos");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(conRuta).build();
        servidor.expect(requestTo("http://pedidos/api/v1/orders/" + PEDIDO + "/payment-result"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.codigoPago").value(pago.getCodigoPago().toString()))
                .andRespond(withSuccess());
        assertTrue(new PedidosClientRest(conRuta.build(), "/api/v1/orders/{codigoPedido}/payment-result")
                .avisarResultadoPago(pago));
        servidor.verify();
    }
}
