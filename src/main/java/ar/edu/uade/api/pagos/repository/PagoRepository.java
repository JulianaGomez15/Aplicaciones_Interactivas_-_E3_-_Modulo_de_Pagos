package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ar.edu.uade.api.pagos.model.EstadoPago;
import java.util.Collection;

import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Pago findByCodigoPago(UUID codigoPago);

    Pago findByClaveControl(String claveControl);

    // RN-03: un pedido no puede tener dos pagos vivos. Se excluye la clave propia para no romper la idempotencia.
    boolean existsByCodigoPedidoAndEstadoInAndClaveControlNot(UUID codigoPedido, Collection<EstadoPago> estados,
                                                              String claveControl);

    // Lectura con bloqueo de escritura: serializa reembolsos simultaneos sobre el mismo pago.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.codigoPago = :codigoPago")
    Pago buscarParaActualizar(@Param("codigoPago") UUID codigoPago);
}
