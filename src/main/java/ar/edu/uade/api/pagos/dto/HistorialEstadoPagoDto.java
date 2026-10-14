package ar.edu.uade.api.pagos.dto;

import java.time.LocalDateTime;

public class HistorialEstadoPagoDto {

    private final String estadoAnterior;
    private final String estadoNuevo;
    private final String motivo;
    private final LocalDateTime fecha;

    public HistorialEstadoPagoDto(String estadoAnterior, String estadoNuevo, String motivo, LocalDateTime fecha) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }
}
