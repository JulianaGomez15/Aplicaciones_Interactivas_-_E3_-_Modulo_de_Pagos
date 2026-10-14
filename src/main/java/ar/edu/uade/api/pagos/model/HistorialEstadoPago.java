package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_estado_pago")
public class HistorialEstadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Null cuando es el alta del pago.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPago estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPago estadoNuevo;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialEstadoPago() {
    }

    public HistorialEstadoPago(EstadoPago estadoAnterior, EstadoPago estadoNuevo, String motivo) {
        this.codigo = UUID.randomUUID();
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public Pago getPago() { return pago; }
    void setPago(Pago pago) { this.pago = pago; }
    public EstadoPago getEstadoAnterior() { return estadoAnterior; }
    public EstadoPago getEstadoNuevo() { return estadoNuevo; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "HistorialEstadoPago{id=" + id + ", " + estadoAnterior + " -> " + estadoNuevo + "}";
    }
}
