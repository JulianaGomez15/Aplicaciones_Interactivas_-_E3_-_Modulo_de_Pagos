package ar.edu.uade.api.pagos.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medios_pago")
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

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

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected MedioPago() {
    }

    public MedioPago(UUID codigoUsuario, TipoMedioPago tipo, String detalleEnmascarado) {
        this.codigo = UUID.randomUUID();
        this.codigoUsuario = codigoUsuario;
        this.tipo = tipo;
        this.detalleEnmascarado = detalleEnmascarado;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getCodigo() { return codigo; }
    public UUID getCodigoUsuario() { return codigoUsuario; }
    public TipoMedioPago getTipo() { return tipo; }
    public String getDetalleEnmascarado() { return detalleEnmascarado; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    @Override
    public String toString() {
        return "MedioPago{id=" + id + ", codigo=" + codigo + ", tipo=" + tipo + "}";
    }
}
