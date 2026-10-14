package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reembolsos")
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Reembolso() {
    }

    public Reembolso(BigDecimal monto, String motivo) {
        this.codigo = UUID.randomUUID();
        this.monto = monto;
        this.motivo = motivo;
        this.estado = EstadoReembolso.APROBADO; // simulado
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public Pago getPago() { return pago; }
    void setPago(Pago pago) { this.pago = pago; }
    public BigDecimal getMonto() { return monto; }
    public String getMotivo() { return motivo; }
    public EstadoReembolso getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "Reembolso{id=" + id + ", codigo=" + codigo + ", monto=" + monto + "}";
    }
}
