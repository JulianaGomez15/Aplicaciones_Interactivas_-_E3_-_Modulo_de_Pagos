package ar.edu.uade.api.pagos.model;

public enum TipoMedioPago {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    BILLETERA_VIRTUAL,
    TRANSFERENCIA;

    public boolean esTarjeta() {
        return this == TARJETA_CREDITO || this == TARJETA_DEBITO;
    }
}
