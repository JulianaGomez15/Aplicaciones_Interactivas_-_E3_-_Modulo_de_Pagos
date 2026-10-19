package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "medio_pago")
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_medio_pago", nullable = false, unique = true, updatable = false)
    private UUID codigoMedioPago;

    // Referencia al modulo de Usuarios: UUID sin FK (cada modulo tiene su propia base).
    @Column(name = "codigo_usuario", nullable = false)
    private UUID codigoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMedioPago tipo;

    // Solo se guarda el detalle enmascarado (RN-08), nunca el dato real.
    @Column(name = "detalle_enmascarado", nullable = false)
    private String detalleEnmascarado;

    @Column(nullable = false)
    private Boolean activo = true;

    protected MedioPago() {
    }

    public MedioPago(UUID codigoUsuario, TipoMedioPago tipo, String detalleEnmascarado) {
        this.codigoUsuario = codigoUsuario;
        this.tipo = tipo;
        this.detalleEnmascarado = detalleEnmascarado;
        this.activo = true;
    }

    @PrePersist
    void prePersist() {
        if (codigoMedioPago == null) {
            codigoMedioPago = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }
    public UUID getCodigoMedioPago() { return codigoMedioPago; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public TipoMedioPago getTipo() { return tipo; }
    public String getDetalleEnmascarado() { return detalleEnmascarado; }
    public Boolean getActivo() { return activo; }

    @Override
    public String toString() {
        return "MedioPago{id=" + id + ", codigoMedioPago=" + codigoMedioPago + ", tipo=" + tipo + "}";
    }
}
