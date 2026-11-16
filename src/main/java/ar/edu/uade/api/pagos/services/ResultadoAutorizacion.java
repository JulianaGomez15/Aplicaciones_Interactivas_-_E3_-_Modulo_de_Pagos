package ar.edu.uade.api.pagos.services;

import ar.edu.uade.api.pagos.model.EstadoPago;

// Los cuatro resultados posibles de la autorizacion simulada y el estado en que dejan el pago.
public enum ResultadoAutorizacion {

    APROBADO(EstadoPago.APROBADO, "pago ok"),
    FONDOS_INSUFICIENTES(EstadoPago.RECHAZADO, "rechazado: fondos insuficientes"),
    NO_AUTORIZADO(EstadoPago.NO_AUTORIZADO, "no autorizado por el banco"),
    ERROR(EstadoPago.ERROR, "error: intente de nuevo");

    private final EstadoPago estado;
    private final String descripcion;

    ResultadoAutorizacion(EstadoPago estado, String descripcion) {
        this.estado = estado;
        this.descripcion = descripcion;
    }

    public EstadoPago getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
}
