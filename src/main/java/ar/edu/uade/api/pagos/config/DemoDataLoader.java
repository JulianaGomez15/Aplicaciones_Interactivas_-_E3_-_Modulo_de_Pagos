package ar.edu.uade.api.pagos.config;

import ar.edu.uade.api.pagos.dto.*;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import ar.edu.uade.api.pagos.integration.PedidosClientSimulado;
import ar.edu.uade.api.pagos.integration.UsuariosClientSimulado;
import ar.edu.uade.api.pagos.services.MedioPagoServices;
import ar.edu.uade.api.pagos.services.PagoServices;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.function.Supplier;

// Carga datos de demo SIEMPRE a traves de los Services (nunca directo a los Repositories).
@Configuration
public class DemoDataLoader {

    @Bean
    CommandLineRunner demo(MedioPagoServices medioPagoServices, PagoServices pagoServices) {

        return args -> {

            MedioPagoDto tarjetaAna = medioPagoServices.crearMedioPago(
                    new MedioPagoRequestDto(UsuariosClientSimulado.USUARIO_ANA, "TARJETA_CREDITO", "4111 1111 1111 1234"));
            MedioPagoDto billeteraBruno = medioPagoServices.crearMedioPago(
                    new MedioPagoRequestDto(UsuariosClientSimulado.USUARIO_BRUNO, "BILLETERA_VIRTUAL", "bruno.diaz@billetera"));

            // Pedido 1: pago aprobado y reembolsado parcialmente.
            PagoDto aprobado = pagoServices.iniciarPago(new IniciarPagoRequestDto(
                    PedidosClientSimulado.PEDIDO_1, UsuariosClientSimulado.USUARIO_ANA, tarjetaAna.getCodigo(),
                    new BigDecimal("15000.00"), "demo-clave-001")).getPago();
            pagoServices.aprobarPago(aprobado.getCodigo());
            pagoServices.reembolsarPago(aprobado.getCodigo(),
                    new ReembolsoRequestDto(new BigDecimal("5000.00"), "Producto faltante"));

            // Pedido 2: pago rechazado (puede reintentarse con una clave nueva).
            PagoDto rechazado = pagoServices.iniciarPago(new IniciarPagoRequestDto(
                    PedidosClientSimulado.PEDIDO_2, UsuariosClientSimulado.USUARIO_BRUNO, billeteraBruno.getCodigo(),
                    new BigDecimal("8500.50"), "demo-clave-002")).getPago();
            pagoServices.rechazarPago(rechazado.getCodigo(), new RechazarPagoRequestDto());

            System.out.println("\n=== CASOS INVALIDOS (deben rechazarse) ===");

            intentar("Clave repetida con datos distintos", () -> pagoServices.iniciarPago(new IniciarPagoRequestDto(
                    PedidosClientSimulado.PEDIDO_3, UsuariosClientSimulado.USUARIO_ANA, tarjetaAna.getCodigo(),
                    new BigDecimal("32000.00"), "demo-clave-001")));

            intentar("Monto distinto al total del pedido", () -> pagoServices.iniciarPago(new IniciarPagoRequestDto(
                    PedidosClientSimulado.PEDIDO_3, UsuariosClientSimulado.USUARIO_ANA, tarjetaAna.getCodigo(),
                    new BigDecimal("100.00"), "demo-clave-003")));

            intentar("Reembolso mayor al disponible", () -> pagoServices.reembolsarPago(aprobado.getCodigo(),
                    new ReembolsoRequestDto(new BigDecimal("10000.01"), "Excedido")));

            intentar("Aprobar un pago RECHAZADO", () -> pagoServices.aprobarPago(rechazado.getCodigo()));

            System.out.println("\nMisma clave y mismos datos -> devuelve el pago original: "
                    + pagoServices.iniciarPago(new IniciarPagoRequestDto(
                    PedidosClientSimulado.PEDIDO_1, UsuariosClientSimulado.USUARIO_ANA, tarjetaAna.getCodigo(),
                    new BigDecimal("15000.00"), "demo-clave-001")).getPago().getCodigo());

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
