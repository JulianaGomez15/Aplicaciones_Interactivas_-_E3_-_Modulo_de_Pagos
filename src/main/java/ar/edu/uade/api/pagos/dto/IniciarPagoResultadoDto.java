package ar.edu.uade.api.pagos.dto;

// Resultado interno de iniciarPago: distingue pago nuevo (201) de pago ya existente por idempotencia (200).
public class IniciarPagoResultadoDto {

    private final PagoDto pago;
    private final boolean creado;

    public IniciarPagoResultadoDto(PagoDto pago, boolean creado) {
        this.pago = pago;
        this.creado = creado;
    }

    public PagoDto getPago() { return pago; }
    public boolean isCreado() { return creado; }
}
