package ar.edu.uade.api.pagos.model;

public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    // El banco no autorizo el pago (resultado de la autorizacion simulada).
    NO_AUTORIZADO,
    // Fallo al procesar el pago: se reintenta con una clave de control nueva.
    ERROR,
    REEMBOLSADO;

    // RN-04: unicas transiciones validas.
    public boolean permiteTransicionA(EstadoPago destino) {
        return switch (this) {
            case PENDIENTE -> destino == APROBADO || destino == RECHAZADO || destino == NO_AUTORIZADO
                    || destino == ERROR;
            case APROBADO -> destino == REEMBOLSADO;
            case RECHAZADO, NO_AUTORIZADO, ERROR, REEMBOLSADO -> false;
        };
    }
}
