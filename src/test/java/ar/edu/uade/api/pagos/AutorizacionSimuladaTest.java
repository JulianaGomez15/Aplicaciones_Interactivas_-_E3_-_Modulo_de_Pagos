package ar.edu.uade.api.pagos;

import ar.edu.uade.api.pagos.dto.CrearPagoRequest;
import ar.edu.uade.api.pagos.dto.MedioPagoRequest;
import ar.edu.uade.api.pagos.dto.PagoResponse;
import ar.edu.uade.api.pagos.dto.ResultadoOperacion;
import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.PedidosClientSimulado;
import ar.edu.uade.api.pagos.integration.UsuariosClientSimulado;
import ar.edu.uade.api.pagos.services.AutorizadorSimulado;
import ar.edu.uade.api.pagos.services.MedioPagoService;
import ar.edu.uade.api.pagos.services.PagoService;
import ar.edu.uade.api.pagos.services.ResultadoAutorizacion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Autorizacion simulada: reparto de los pesos y efecto de cada resultado sobre el pago.
class AutorizacionSimuladaTest {

    // ---------------------------------------------------------------- reparto de pesos (sin Spring)

    @Test
    void repartoPorDefecto70_10_10_10() {
        AutorizadorSimulado autorizador = new AutorizadorSimulado(true, 70, 10, 10, 10);

        assertEquals(100, autorizador.total());
        assertEquals(ResultadoAutorizacion.APROBADO, autorizador.resolver(1));
        assertEquals(ResultadoAutorizacion.APROBADO, autorizador.resolver(70));
        assertEquals(ResultadoAutorizacion.FONDOS_INSUFICIENTES, autorizador.resolver(71));
        assertEquals(ResultadoAutorizacion.FONDOS_INSUFICIENTES, autorizador.resolver(80));
        assertEquals(ResultadoAutorizacion.NO_AUTORIZADO, autorizador.resolver(81));
        assertEquals(ResultadoAutorizacion.NO_AUTORIZADO, autorizador.resolver(90));
        assertEquals(ResultadoAutorizacion.ERROR, autorizador.resolver(91));
        assertEquals(ResultadoAutorizacion.ERROR, autorizador.resolver(100));
        assertThrows(IllegalArgumentException.class, () -> autorizador.resolver(0));
        assertThrows(IllegalArgumentException.class, () -> autorizador.resolver(101));
    }

    @Test
    void elSorteoRespetaLosPesos() {
        AutorizadorSimulado autorizador = new AutorizadorSimulado(true, 70, 10, 10, 10);
        Map<ResultadoAutorizacion, Integer> cuenta = new EnumMap<>(ResultadoAutorizacion.class);
        int sorteos = 20_000;
        for (int i = 0; i < sorteos; i++) {
            AutorizadorSimulado.Sorteo sorteo = autorizador.sortear();
            assertEquals(autorizador.resolver(sorteo.numero()), sorteo.resultado());
            assertNotNull(sorteo.motivo());
            cuenta.merge(sorteo.resultado(), 1, Integer::sum);
        }
        // Margen amplio: con 20.000 sorteos el desvio esperado es de menos de medio punto.
        assertEquals(0.70, cuenta.get(ResultadoAutorizacion.APROBADO) / (double) sorteos, 0.03);
        assertEquals(0.10, cuenta.get(ResultadoAutorizacion.FONDOS_INSUFICIENTES) / (double) sorteos, 0.03);
        assertEquals(0.10, cuenta.get(ResultadoAutorizacion.NO_AUTORIZADO) / (double) sorteos, 0.03);
        assertEquals(0.10, cuenta.get(ResultadoAutorizacion.ERROR) / (double) sorteos, 0.03);
    }

    @Test
    void pesosInvalidosNoArrancan() {
        assertThrows(IllegalArgumentException.class, () -> new AutorizadorSimulado(true, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AutorizadorSimulado(true, -1, 10, 10, 10));
    }

    // ---------------------------------------------------------------- cada resultado, de punta a punta

    abstract static class Base {
        @Autowired
        PagoService pagoService;
        @Autowired
        MedioPagoService medioPagoService;

        PagoResponse pagar(String clave) {
            ResultadoOperacion<PagoResponse> resultado = pagoService.iniciarPago(solicitud(clave));
            assertTrue(resultado.isCreado());
            return resultado.getCuerpo();
        }

        CrearPagoRequest solicitud(String clave) {
            UUID medio = medioPagoService.registrarMedioPago(UsuariosClientSimulado.USUARIO_ANA,
                    new MedioPagoRequest("TARJETA_CREDITO", "4111 1111 1111 1234")).getCodigoMedioPago();
            return solicitud(clave, medio);
        }

        CrearPagoRequest solicitud(String clave, UUID medio) {
            return new CrearPagoRequest(PedidosClientSimulado.PEDIDO_3, UsuariosClientSimulado.USUARIO_ANA, medio,
                    new BigDecimal("32000.00"), "ARS", clave);
        }
    }

    @Nested
    @SpringBootTest(properties = {"integraciones.modo=simulado", "autorizacion.peso.aprobado=1",
            "autorizacion.peso.fondos-insuficientes=0", "autorizacion.peso.no-autorizado=0",
            "autorizacion.peso.error=0"})
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class SiempreAprobado extends Base {

        @Test
        void quedaAprobadoYSePuedeReembolsar() {
            PagoResponse pago = pagar("sorteo-ok");

            assertEquals("APROBADO", pago.getEstado());
            assertNull(pago.getMotivo());
            // alta + aprobacion
            assertEquals(2, pagoService.consultarHistorial(pago.getCodigoPago()).size());
            // Aprobar de nuevo no cambia nada (RN-10); rechazarlo ya no es valido (RN-04)
            assertEquals("APROBADO", pagoService.aprobarPago(pago.getCodigoPago()).getEstado());
            PagoException e = assertThrows(PagoException.class,
                    () -> pagoService.rechazarPago(pago.getCodigoPago(), null));
            assertEquals(ErrorCode.PAYMENT_INVALID_STATE, e.getCode());
        }

        @Test
        void repetirLaClaveDevuelveElMismoPagoSinVolverASortear() {
            UUID medio = medioPagoService.registrarMedioPago(UsuariosClientSimulado.USUARIO_ANA,
                    new MedioPagoRequest("TARJETA_CREDITO", "4111 1111 1111 1234")).getCodigoMedioPago();
            PagoResponse primero = pagoService.iniciarPago(solicitud("sorteo-repetido", medio)).getCuerpo();
            ResultadoOperacion<PagoResponse> segundo = pagoService.iniciarPago(solicitud("sorteo-repetido", medio));

            assertFalse(segundo.isCreado());
            assertEquals(primero.getCodigoPago(), segundo.getCuerpo().getCodigoPago());
            assertEquals("APROBADO", segundo.getCuerpo().getEstado());
            assertEquals(2, pagoService.consultarHistorial(primero.getCodigoPago()).size());
        }
    }

    @Nested
    @SpringBootTest(properties = {"integraciones.modo=simulado", "autorizacion.peso.aprobado=0",
            "autorizacion.peso.fondos-insuficientes=1", "autorizacion.peso.no-autorizado=0",
            "autorizacion.peso.error=0"})
    class SiempreFondosInsuficientes extends Base {

        @Test
        void quedaRechazadoConElMotivo() {
            PagoResponse pago = pagar("sorteo-fondos");

            assertEquals("RECHAZADO", pago.getEstado());
            assertEquals("Fondos insuficientes", pago.getMotivo());
            assertEquals("Fondos insuficientes",
                    pagoService.consultarHistorial(pago.getCodigoPago()).get(1).getMotivo());
        }
    }

    @Nested
    @SpringBootTest(properties = {"integraciones.modo=simulado", "autorizacion.peso.aprobado=0",
            "autorizacion.peso.fondos-insuficientes=0", "autorizacion.peso.no-autorizado=1",
            "autorizacion.peso.error=0"})
    class SiempreNoAutorizado extends Base {

        @Test
        void quedaNoAutorizadoYNoSePuedeAprobarDespues() {
            PagoResponse pago = pagar("sorteo-banco");

            assertEquals("NO_AUTORIZADO", pago.getEstado());
            assertTrue(pago.getMotivo().startsWith("El banco no autorizo el pago: "), pago.getMotivo());
            assertEquals("NO_AUTORIZADO", pagoService.consultarPago(pago.getCodigoPago(), null).getEstado());
            PagoException e = assertThrows(PagoException.class, () -> pagoService.aprobarPago(pago.getCodigoPago()));
            assertEquals(ErrorCode.PAYMENT_INVALID_STATE, e.getCode());
        }
    }

    @Nested
    @SpringBootTest(properties = {"integraciones.modo=simulado", "autorizacion.peso.aprobado=0",
            "autorizacion.peso.fondos-insuficientes=0", "autorizacion.peso.no-autorizado=0",
            "autorizacion.peso.error=1"})
    class SiempreError extends Base {

        @Test
        void quedaEnErrorYSeReintentaConOtraClave() {
            PagoResponse pago = pagar("sorteo-error");

            assertEquals("ERROR", pago.getEstado());
            assertEquals("Error al procesar el pago: intente de nuevo", pago.getMotivo());
            // "Intente de nuevo": el mismo pedido admite otro intento con una clave nueva
            PagoResponse reintento = pagar("sorteo-error-2");
            assertNotEquals(pago.getCodigoPago(), reintento.getCodigoPago());
        }
    }
}
