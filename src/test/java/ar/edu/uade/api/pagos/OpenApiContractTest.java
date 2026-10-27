package ar.edu.uade.api.pagos;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Verifica que el Swagger generado respete el contrato del TPO (tabla "APIs", DTOs y manejo de errores).
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    MockMvc mvc;

    static Object docs;

    @BeforeAll
    static void limpiar() {
        docs = null;
    }

    private Object docs() throws Exception {
        if (docs == null) {
            String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
            docs = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        }
        return docs;
    }

    private <T> T leer(String path) throws Exception {
        return JsonPath.read(docs(), path);
    }

    private boolean existe(String path) throws Exception {
        try {
            JsonPath.read(docs(), path);
            return true;
        } catch (com.jayway.jsonpath.PathNotFoundException e) {
            return false;
        }
    }

    private Set<String> codigos(String ruta, String metodo) throws Exception {
        Map<String, Object> responses = leer("$.paths['" + ruta + "']." + metodo + ".responses");
        return responses.keySet();
    }

    // Tabla "APIs" del TPO: ID, metodo, ruta y codigos HTTP.
    @Test
    void cadaApiExisteConLosCodigosHttpDelTpo() throws Exception {
        assertEquals(Set.of("200", "201", "400", "401", "403", "409", "422", "503"),
                codigos("/api/v1/payments", "post"));                                       // API-01
        assertEquals(Set.of("200", "401", "403", "404", "409"),
                codigos("/api/v1/payments/{codigoPago}/approve", "post"));                  // API-02
        assertEquals(Set.of("200", "400", "401", "403", "404", "409"),
                codigos("/api/v1/payments/{codigoPago}/reject", "post"));                   // API-03
        assertEquals(Set.of("200", "401", "403", "404"),
                codigos("/api/v1/payments/{codigoPago}", "get"));                           // API-04
        assertEquals(Set.of("200", "201", "400", "401", "403", "404", "409", "422"),
                codigos("/api/v1/payments/{codigoPago}/refund", "post"));                   // API-05
        assertEquals(Set.of("200", "400", "401", "403"),
                codigos("/api/v1/payments", "get"));                                        // API-06
        assertEquals(Set.of("201", "400", "401", "403"),
                codigos("/api/v1/payment-methods", "post"));                                // API-07
        assertEquals(Set.of("200", "401", "403", "404"),
                codigos("/api/v1/payments/{codigoPago}/history", "get"));                   // API-08
    }

    @Test
    void soloExponeLosEndpointsDelContrato() throws Exception {
        Map<String, Object> paths = leer("$.paths");
        assertEquals(Set.of("/api/v1/payments", "/api/v1/payments/{codigoPago}",
                        "/api/v1/payments/{codigoPago}/approve", "/api/v1/payments/{codigoPago}/reject",
                        "/api/v1/payments/{codigoPago}/refund", "/api/v1/payments/{codigoPago}/history",
                        "/api/v1/payment-methods", "/auth/login"),
                paths.keySet());
    }

    @Test
    void operacionesTienenIdResumenYSeguridad() throws Exception {
        Map<String, String> ids = Map.of(
                "$.paths['/api/v1/payments'].post", "API-01_iniciarPago",
                "$.paths['/api/v1/payments/{codigoPago}/approve'].post", "API-02_aprobarPago",
                "$.paths['/api/v1/payments/{codigoPago}/reject'].post", "API-03_rechazarPago",
                "$.paths['/api/v1/payments/{codigoPago}'].get", "API-04_consultarPago",
                "$.paths['/api/v1/payments/{codigoPago}/refund'].post", "API-05_reembolsarPago",
                "$.paths['/api/v1/payments'].get", "API-06_listarPagos",
                "$.paths['/api/v1/payment-methods'].post", "API-07_registrarMedioPago",
                "$.paths['/api/v1/payments/{codigoPago}/history'].get", "API-08_consultarHistorial");
        for (var e : ids.entrySet()) {
            assertEquals(e.getValue(), leer(e.getKey() + ".operationId"));
            String resumen = leer(e.getKey() + ".summary");
            assertTrue(resumen.contains("CU-"), "el resumen debe citar el CU: " + resumen);
            String descripcion = leer(e.getKey() + ".description");
            assertTrue(descripcion.contains("Rol"), "la descripcion debe indicar los roles: " + e.getKey());
            JSONArray seguridad = leer(e.getKey() + ".security");
            assertFalse(seguridad.isEmpty(), "debe exigir bearerAuth: " + e.getKey());
        }
        assertEquals("bearer", leer("$.components.securitySchemes.bearerAuth.scheme"));
        assertEquals("JWT", leer("$.components.securitySchemes.bearerAuth.bearerFormat"));
    }

    @Test
    void erroresUsanElEsquemaNormalizado() throws Exception {
        assertEquals("#/components/schemas/ErrorResponse",
                leer("$.paths['/api/v1/payments/{codigoPago}/approve'].post.responses['409'].content"
                        + "['application/json'].schema['$ref']"));
        assertEquals("#/components/schemas/ErrorResponse",
                leer("$.paths['/api/v1/payments'].post.responses['503'].content['application/json'].schema['$ref']"));
        Map<String, Object> props = leer("$.components.schemas.ErrorResponse.properties");
        assertEquals(Set.of("timestamp", "status", "code", "message", "path", "correlationId"), props.keySet());
    }

    @Test
    void dtosCoincidenConElTpo() throws Exception {
        assertEquals(Set.of("codigoPedido", "codigoUsuario", "codigoMedioPago", "monto", "moneda", "claveControl"),
                propiedades("CrearPagoRequest"));                                            // DTO-01
        assertEquals(Set.of("codigoPago", "codigoPedido", "estado", "monto", "moneda", "medioPago",
                "fechaCreacion", "fechaActualizacion"), propiedades("PagoResponse"));        // DTO-02
        assertEquals(Set.of("tipo", "detalleEnmascarado"), propiedades("MedioPagoResumenResponse"));
        assertEquals(Set.of("motivo"), propiedades("RechazarPagoRequest"));                  // DTO-03
        assertEquals(Set.of("monto", "motivo", "claveControl"), propiedades("ReembolsoRequest")); // DTO-04
        assertEquals(Set.of("codigoReembolso", "codigoPago", "monto", "estado", "montoDisponible",
                "fechaSolicitud", "fechaResolucion"), propiedades("ReembolsoResponse"));     // DTO-05
        assertEquals(Set.of("content", "page", "size", "totalElements", "totalPages"),
                propiedades("PaginaPagosResponse"));                                         // DTO-06
        assertEquals(Set.of("tipo", "numero"), propiedades("MedioPagoRequest"));             // DTO-07
        assertEquals(Set.of("codigoMedioPago", "tipo", "detalleEnmascarado", "activo"),
                propiedades("MedioPagoResponse"));                                           // DTO-08
        assertEquals(Set.of("estadoAnterior", "estadoNuevo", "motivo", "fecha"),
                propiedades("HistorialEstadoResponse"));                                     // DTO-09
        // El id interno y el codigo de usuario del JWT nunca viajan en los cuerpos
        assertFalse(propiedades("MedioPagoRequest").contains("codigoUsuario"));
    }

    @Test
    void parametrosDocumentadosYSinFiltracionesInternas() throws Exception {
        // Solo el codigo de pago: el principal del JWT no debe aparecer como parametro ni como body
        List<Map<String, Object>> consulta = leer("$.paths['/api/v1/payments/{codigoPago}'].get.parameters");
        assertEquals(1, consulta.size());
        assertEquals("codigoPago", consulta.get(0).get("name"));
        assertNotNull(consulta.get(0).get("description"));
        assertFalse(existe("$.paths['/api/v1/payments/{codigoPago}'].get.requestBody"));
        assertFalse(existe("$.paths['/api/v1/payment-methods'].post.parameters"));

        List<Map<String, Object>> listado = leer("$.paths['/api/v1/payments'].get.parameters");
        Set<String> nombres = new java.util.HashSet<>();
        for (Map<String, Object> p : listado) {
            nombres.add((String) p.get("name"));
        }
        assertTrue(nombres.containsAll(Set.of("estado", "codigoUsuario", "codigoPedido", "desde", "hasta",
                "page", "size", "sort")), "parametros del listado: " + nombres);
        assertFalse(nombres.contains("pageable"));
        for (Map<String, Object> p : listado) {
            if (Set.of("estado", "codigoUsuario", "codigoPedido", "desde", "hasta").contains(p.get("name"))) {
                assertNotNull(p.get("description"), "falta descripcion de " + p.get("name"));
            }
        }
    }

    // Swagger UI es la unica interfaz visual del modulo: tiene que abrir sin JWT.
    @Test
    void swaggerUiEsPublicoYCarga() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        String config = mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(config.contains("/v3/api-docs"), "swagger-config debe apuntar a /v3/api-docs: " + config);
        // El contrato tiene que poder descargarse sin token, en JSON y en YAML.
        mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        // El login de demo tambien queda documentado y es publico, para poder obtener el token desde Swagger.
        assertTrue(existe("$.paths['/auth/login'].post"));
    }

    @Test
    void tagsEInformacionGeneral() throws Exception {
        assertEquals("DeliveryGo - Modulo de Pagos", leer("$.info.title"));
        assertEquals(List.of("Pagos"), leer("$.paths['/api/v1/payments'].post.tags"));
        assertEquals(List.of("Medios de pago"), leer("$.paths['/api/v1/payment-methods'].post.tags"));
    }

    @SuppressWarnings("unchecked")
    private Set<String> propiedades(String schema) throws Exception {
        Map<String, Object> props = leer("$.components.schemas." + schema + ".properties");
        return props.keySet();
    }
}
