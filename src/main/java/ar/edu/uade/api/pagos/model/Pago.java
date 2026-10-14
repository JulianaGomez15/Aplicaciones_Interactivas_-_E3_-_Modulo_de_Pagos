package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador publico (se expone en la API); el id interno nunca sale.
    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

    @Column(name = "codigo_pedido", nullable = false)
    private UUID codigoPedido;

    @Column(name = "codigo_usuario", nullable = false)
    private UUID codigoUsuario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "ARS";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    // RN-01: UNIQUE a nivel de base ademas de la validacion en el Service.
    @Column(name = "clave_control", nullable = false, unique = true)
    private String claveControl;

    @ManyToOne
    @JoinColumn(name = "medio_pago_id", nullable = false)
    private MedioPago medioPago;

    @OneToMany(mappedBy = "pago", fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<Reembolso> reembolsos = new ArrayList<>();

    @OneToMany(mappedBy = "pago", fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<HistorialEstadoPago> historial = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // Evita que dos operaciones concurrentes pisen el estado del mismo pago.
    @Version
    private Long version;

    protected Pago() {
    }

    public Pago(UUID codigoPedido, UUID codigoUsuario, BigDecimal monto, String claveControl, MedioPago medioPago) {
        this.codigo = UUID.randomUUID();
        this.codigoPedido = codigoPedido;
        this.codigoUsuario = codigoUsuario;
        this.monto = monto;
        this.claveControl = claveControl;
        this.medioPago = medioPago;
        this.estado = EstadoPago.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    // Mantiene sincronizada la relacion bidireccional en memoria.
    public void agregarReembolso(Reembolso reembolso) {
        reembolsos.add(reembolso);
        reembolso.setPago(this);
    }

    public void agregarHistorial(HistorialEstadoPago registro) {
        historial.add(registro);
        registro.setPago(this);
    }

    public BigDecimal totalReembolsado() {
        return reembolsos.stream().map(Reembolso::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal montoDisponibleParaReembolso() {
        return monto.subtract(totalReembolsado());
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public UUID getCodigoPedido() { return codigoPedido; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public EstadoPago getEstado() { return estado; }
    public String getClaveControl() { return claveControl; }
    public MedioPago getMedioPago() { return medioPago; }
    public List<Reembolso> getReembolsos() { return reembolsos; }
    public List<HistorialEstadoPago> getHistorial() { return historial; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    @Override
    public String toString() {
        return "Pago{id=" + id + ", codigo=" + codigo + ", estado=" + estado + ", monto=" + monto + "}";
    }
}
