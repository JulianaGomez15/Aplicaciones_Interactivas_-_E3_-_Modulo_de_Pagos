package ar.edu.uade.api.pagos.model;

public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    REEMBOLSADO;

    // RN-04: unicas transiciones validas.
    public boolean permiteTransicionA(EstadoPago destino) {
        return switch (this) {
            case PENDIENTE -> destino == APROBADO || destino == RECHAZADO;
            case APROBADO -> destino == REEMBOLSADO;
            case RECHAZADO, REEMBOLSADO -> false;
        };
    }
}
