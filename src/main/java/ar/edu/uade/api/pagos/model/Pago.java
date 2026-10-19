package ar.edu.uade.api.pagos.model;

import ar.edu.uade.api.pagos.excepcion.ErrorCode;
import ar.edu.uade.api.pagos.excepcion.PagoException;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador publico (se expone en la API); el id interno nunca sale.
    @Column(name = "codigo_pago", nullable = false, unique = true, updatable = false)
    private UUID codigoPago;

    // Referencias a otros modulos: UUID sin FK.
    @Column(name = "codigo_pedido", nullable = false)
    private UUID codigoPedido;

    @Column(name = "codigo_usuario", nullable = false)
    private UUID codigoUsuario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    // RN-01: UNIQUE a nivel de base ademas de la validacion en el Service (cubre solicitudes simultaneas).
    @Column(name = "clave_control", nullable = false, unique = true)
    private String claveControl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_pago_id", nullable = false)
    private MedioPago medioPago;

    @OneToMany(mappedBy = "pago", fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<Reembolso> reembolsos = new ArrayList<>();

    @OneToMany(mappedBy = "pago", fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<HistorialEstadoPago> historial = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // DEC-05: bloqueo optimista para que dos operaciones simultaneas no generen dos transiciones.
    @Version
    private Long version;

    protected Pago() {
    }

    public Pago(UUID codigoPedido, UUID codigoUsuario, BigDecimal monto, String moneda, String claveControl,
                MedioPago medioPago) {
        this.codigoPedido = codigoPedido;
        this.codigoUsuario = codigoUsuario;
        this.monto = monto;
        this.moneda = moneda;
        this.claveControl = claveControl;
        this.medioPago = medioPago;
        this.estado = EstadoPago.PENDIENTE;
    }

    @PrePersist
    void prePersist() {
        if (codigoPago == null) {
            codigoPago = UUID.randomUUID();
        }
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // ---- Reglas que dependen solo del estado (RN-04 y RN-06) ----

    public void aprobar() {
        transicionarA(EstadoPago.APROBADO);
    }

    public void rechazar() {
        transicionarA(EstadoPago.RECHAZADO);
    }

    public void marcarReembolsado() {
        transicionarA(EstadoPago.REEMBOLSADO);
    }

    public boolean puedeReembolsarse() {
        return estado == EstadoPago.APROBADO;
    }

    private void transicionarA(EstadoPago destino) {
        if (!estado.permiteTransicionA(destino)) {
            throw new PagoException(ErrorCode.PAYMENT_INVALID_STATE,
                    "Transicion invalida: " + estado + " -> " + destino);
        }
        this.estado = destino;
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

    public Long getId() { return id; }
    public UUID getCodigoPago() { return codigoPago; }
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
        return "Pago{id=" + id + ", codigoPago=" + codigoPago + ", estado=" + estado + ", monto=" + monto + "}";
    }
}
