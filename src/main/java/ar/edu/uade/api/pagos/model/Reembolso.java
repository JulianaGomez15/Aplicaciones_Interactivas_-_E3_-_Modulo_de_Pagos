package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reembolso")
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reembolso", nullable = false, unique = true, updatable = false)
    private UUID codigoReembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado;

    // RN-10: un reembolso repetido con la misma clave devuelve el existente.
    @Column(name = "clave_control", nullable = false, unique = true)
    private String claveControl;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    protected Reembolso() {
    }

    // El reembolso es simulado: se solicita y se resuelve (APROBADO) en el mismo instante.
    public Reembolso(BigDecimal monto, String motivo, String claveControl) {
        this.monto = monto;
        this.motivo = motivo;
        this.claveControl = claveControl;
        this.estado = EstadoReembolso.APROBADO;
        this.fechaSolicitud = LocalDateTime.now();
        this.fechaResolucion = this.fechaSolicitud;
    }

    @PrePersist
    void prePersist() {
        if (codigoReembolso == null) {
            codigoReembolso = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }
    public UUID getCodigoReembolso() { return codigoReembolso; }
    public Pago getPago() { return pago; }
    void setPago(Pago pago) { this.pago = pago; }
    public BigDecimal getMonto() { return monto; }
    public String getMotivo() { return motivo; }
    public EstadoReembolso getEstado() { return estado; }
    public String getClaveControl() { return claveControl; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }

    @Override
    public String toString() {
        return "Reembolso{id=" + id + ", codigoReembolso=" + codigoReembolso + ", monto=" + monto + "}";
    }
}
