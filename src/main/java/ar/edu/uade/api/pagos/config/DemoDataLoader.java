package ar.edu.uade.api.pagos.config;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.PedidosClientSimulado;
import ar.edu.uade.api.pagos.integration.UsuariosClientSimulado;
import ar.edu.uade.api.pagos.services.MedioPagoService;
import ar.edu.uade.api.pagos.services.PagoService;
import ar.edu.uade.api.pagos.services.ReembolsoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.function.Supplier;

// Carga datos de demo SIEMPRE a traves de los Services (nunca directo a los Repositories).
@Configuration
@Profile("!postgres") // la base H2 es en memoria y arranca vacia; en PostgreSQL los datos persisten entre arranques
public class DemoDataLoader {

    @Bean
    CommandLineRunner demo(MedioPagoService medioPagoService, PagoService pagoService,
                           ReembolsoService reembolsoService) {

        return args -> {

            MedioPagoResponse tarjetaAna = medioPagoService.registrarMedioPago(UsuariosClientSimulado.USUARIO_ANA,
                    new MedioPagoRequest("TARJETA_CREDITO", "4111 1111 1111 1234"));
            MedioPagoResponse billeteraBruno = medioPagoService.registrarMedioPago(
                    UsuariosClientSimulado.USUARIO_BRUNO, new MedioPagoRequest("BILLETERA_VIRTUAL", "bruno.diaz@billetera"));

            // Pedido 1: pago aprobado y reembolsado parcialmente.
            PagoResponse aprobado = pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_1, UsuariosClientSimulado.USUARIO_ANA, tarjetaAna.getCodigoMedioPago(),
                    new BigDecimal("15000.00"), "ARS", "demo-clave-001")).getCuerpo();
            pagoService.aprobarPago(aprobado.getCodigoPago());
            reembolsoService.reembolsarPago(aprobado.getCodigoPago(),
                    new ReembolsoRequest(new BigDecimal("5000.00"), "Producto faltante", "demo-reembolso-001"));

            // Pedido 2: pago rechazado (puede reintentarse con una clave nueva).
            PagoResponse rechazado = pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_2, UsuariosClientSimulado.USUARIO_BRUNO,
                    billeteraBruno.getCodigoMedioPago(), new BigDecimal("8500.50"), "ARS", "demo-clave-002"))
                    .getCuerpo();
            pagoService.rechazarPago(rechazado.getCodigoPago(), new RechazarPagoRequest("Fondos insuficientes"));

            System.out.println("\n=== CASOS INVALIDOS (deben rechazarse) ===");

            intentar("Clave repetida con datos distintos", () -> pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_3, UsuariosClientSimulado.USUARIO_ANA,
                    tarjetaAna.getCodigoMedioPago(), new BigDecimal("32000.00"), "ARS", "demo-clave-001")));

            intentar("Monto distinto al total del pedido", () -> pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_3, UsuariosClientSimulado.USUARIO_ANA,
                    tarjetaAna.getCodigoMedioPago(), new BigDecimal("100.00"), "ARS", "demo-clave-003")));

            intentar("Pedido que no esta pendiente de pago", () -> pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_CANCELADO, UsuariosClientSimulado.USUARIO_ANA,
                    tarjetaAna.getCodigoMedioPago(), new BigDecimal("4200.00"), "ARS", "demo-clave-004")));

            intentar("Reembolso mayor al disponible", () -> reembolsoService.reembolsarPago(aprobado.getCodigoPago(),
                    new ReembolsoRequest(new BigDecimal("10000.01"), "Excedido", "demo-reembolso-002")));

            intentar("Aprobar un pago RECHAZADO", () -> pagoService.aprobarPago(rechazado.getCodigoPago()));

            System.out.println("\nMisma clave y mismos datos -> devuelve el pago original: "
                    + pagoService.iniciarPago(new CrearPagoRequest(
                    PedidosClientSimulado.PEDIDO_1, UsuariosClientSimulado.USUARIO_ANA,
                    tarjetaAna.getCodigoMedioPago(), new BigDecimal("15000.00"), "ARS", "demo-clave-001"))
                    .getCuerpo().getCodigoPago());

            System.out.println("\nSwagger UI: http://localhost:8089/swagger-ui.html");
            System.out.println("H2 console: http://localhost:8089/h2-console (jdbc:h2:mem:pagos, user sa)");
        };
    }

    private void intentar(String descripcion, Supplier<?> operacion) {
        try {
            operacion.get();
            System.out.println("INESPERADO (se acepto): " + descripcion);
        } catch (PagoException e) {
            System.out.println("Rechazo esperado [" + descripcion + "]: " + e.getCode() + " - " + e.getMessage());
        }
    }
}
