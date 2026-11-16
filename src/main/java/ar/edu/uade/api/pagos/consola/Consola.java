package ar.edu.uade.api.pagos.consola;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// Prints del flujo de pago para la presentacion: un bloque por operacion y una linea por paso
// (solicitud, validaciones, registro, sorteo, aviso a Pedidos, notificacion, resultado).
// Solo ASCII para que se vea igual en cualquier consola; los colores se apagan con consola.colores=false.
@Component
public class Consola {

    private static final int ANCHO = 100;
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    private final boolean colores;
    private final ThreadLocal<Boolean> silencio = ThreadLocal.withInitial(() -> false);

    public Consola(@Value("${consola.colores:true}") boolean colores) {
        this.colores = colores;
    }

    // Encabezado del bloque: nombre de la operacion y correlation id del request.
    public void titulo(String operacion) {
        String correlationId = MDC.get("correlationId");
        String derecha = correlationId == null ? "" : "correlationId " + correlationId;
        imprimir("");
        imprimir(pintar(CYAN, borde('=')));
        imprimir(pintar(CYAN, "| ") + pintar(NEGRITA, operacion)
                + " ".repeat(Math.max(1, ANCHO - 4 - operacion.length() - derecha.length()))
                + derecha + pintar(CYAN, " |"));
        imprimir(pintar(CYAN, borde('=')));
    }

    // Paso informativo (ni exito ni error).
    public void paso(String etapa, String texto) {
        linea(etapa, pintar(AMARILLO, " -> "), texto);
    }

    public void ok(String etapa, String texto) {
        linea(etapa, pintar(VERDE, "[OK]"), texto);
    }

    public void fallo(String etapa, String texto) {
        linea(etapa, pintar(ROJO, "[X] "), texto);
    }

    // Cierre del bloque con el resultado final de la operacion.
    public void resultado(boolean exito, String texto) {
        String contenido = "RESULTADO: " + texto;
        if (contenido.length() > ANCHO - 4) {
            contenido = contenido.substring(0, ANCHO - 7) + "...";
        }
        imprimir(pintar(CYAN, borde('-')));
        imprimir(pintar(CYAN, "| ") + pintar(exito ? VERDE : ROJO, pintar(NEGRITA, contenido))
                + " ".repeat(ANCHO - 4 - contenido.length()) + pintar(CYAN, " |"));
        imprimir(pintar(CYAN, borde('=')));
    }

    // Ejecuta sin imprimir nada (carga de datos de demo al arrancar).
    public <T> T enSilencio(Supplier<T> accion) {
        boolean anterior = silencio.get();
        silencio.set(true);
        try {
            return accion.get();
        } finally {
            silencio.set(anterior);
        }
    }

    private void linea(String etapa, String marca, String texto) {
        imprimir("  " + String.format("%-13s", etapa) + marca + " " + texto);
    }

    private String borde(char relleno) {
        return "+" + String.valueOf(relleno).repeat(ANCHO - 2) + "+";
    }

    private String pintar(String color, String texto) {
        return colores ? color + texto + RESET : texto;
    }

    private void imprimir(String linea) {
        if (!silencio.get()) {
            System.out.println(linea);
        }
    }
}
