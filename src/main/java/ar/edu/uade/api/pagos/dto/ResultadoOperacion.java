package ar.edu.uade.api.pagos.dto;

// Resultado interno de las operaciones idempotentes: distingue "creado" (201) de "ya existia" (200).
public class ResultadoOperacion<T> {

    private final T cuerpo;
    private final boolean creado;

    public ResultadoOperacion(T cuerpo, boolean creado) {
        this.cuerpo = cuerpo;
        this.creado = creado;
    }

    public T getCuerpo() { return cuerpo; }
    public boolean isCreado() { return creado; }
}
