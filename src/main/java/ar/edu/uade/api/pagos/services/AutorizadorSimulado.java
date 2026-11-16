package ar.edu.uade.api.pagos.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Autorizacion simulada: sortea el resultado del pago segun pesos configurables
// (por defecto 70 % ok, 10 % fondos insuficientes, 10 % no autorizado, 10 % error).
@Component
public class AutorizadorSimulado {

    private static final List<String> MOTIVOS_BANCO = List.of(
            "tarjeta vencida", "limite de compra excedido", "sospecha de fraude", "datos de la tarjeta invalidos");

    // Numero que salio, total de numeros posibles, resultado y motivo que se guarda en el pago.
    public record Sorteo(int numero, int total, ResultadoAutorizacion resultado, String motivo) {
    }

    private final boolean automatica;
    private final int pesoAprobado;
    private final int pesoFondos;
    private final int pesoNoAutorizado;
    private final int pesoError;

    public AutorizadorSimulado(@Value("${autorizacion.automatica:true}") boolean automatica,
                               @Value("${autorizacion.peso.aprobado:70}") int pesoAprobado,
                               @Value("${autorizacion.peso.fondos-insuficientes:10}") int pesoFondos,
                               @Value("${autorizacion.peso.no-autorizado:10}") int pesoNoAutorizado,
                               @Value("${autorizacion.peso.error:10}") int pesoError) {
        if (pesoAprobado < 0 || pesoFondos < 0 || pesoNoAutorizado < 0 || pesoError < 0
                || pesoAprobado + pesoFondos + pesoNoAutorizado + pesoError <= 0) {
            throw new IllegalArgumentException("Los pesos de autorizacion.peso.* deben ser >= 0 y sumar mas de 0");
        }
        this.automatica = automatica;
        this.pesoAprobado = pesoAprobado;
        this.pesoFondos = pesoFondos;
        this.pesoNoAutorizado = pesoNoAutorizado;
        this.pesoError = pesoError;
    }

    // true: el pago se resuelve solo al iniciarlo. false: queda PENDIENTE hasta approve / reject.
    public boolean isAutomatica() {
        return automatica;
    }

    public int total() {
        return pesoAprobado + pesoFondos + pesoNoAutorizado + pesoError;
    }

    public Sorteo sortear() {
        int numero = ThreadLocalRandom.current().nextInt(1, total() + 1);
        ResultadoAutorizacion resultado = resolver(numero);
        return new Sorteo(numero, total(), resultado, motivo(resultado));
    }

    // Reparte los numeros 1..total en franjas consecutivas: ok, fondos insuficientes, no autorizado, error.
    public ResultadoAutorizacion resolver(int numero) {
        if (numero < 1 || numero > total()) {
            throw new IllegalArgumentException("El numero debe estar entre 1 y " + total());
        }
        if (numero <= pesoAprobado) {
            return ResultadoAutorizacion.APROBADO;
        }
        if (numero <= pesoAprobado + pesoFondos) {
            return ResultadoAutorizacion.FONDOS_INSUFICIENTES;
        }
        if (numero <= pesoAprobado + pesoFondos + pesoNoAutorizado) {
            return ResultadoAutorizacion.NO_AUTORIZADO;
        }
        return ResultadoAutorizacion.ERROR;
    }

    public String pesos() {
        return pesoAprobado + " ok | " + pesoFondos + " fondos insuficientes | " + pesoNoAutorizado
                + " no autorizado | " + pesoError + " error";
    }

    private String motivo(ResultadoAutorizacion resultado) {
        return switch (resultado) {
            case APROBADO -> "Autorizacion simulada aprobada";
            case FONDOS_INSUFICIENTES -> "Fondos insuficientes";
            case NO_AUTORIZADO -> "El banco no autorizo el pago: "
                    + MOTIVOS_BANCO.get(ThreadLocalRandom.current().nextInt(MOTIVOS_BANCO.size()));
            case ERROR -> "Error al procesar el pago: intente de nuevo";
        };
    }
}
