package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.EstadoPago;
import ar.edu.uade.api.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {
    Pago findByCodigo(UUID codigo);

    Pago findByClaveControl(String claveControl);

    List<Pago> findByCodigoPedidoOrderByFechaCreacionDesc(UUID codigoPedido);

    boolean existsByCodigoPedidoAndEstadoIn(UUID codigoPedido, Collection<EstadoPago> estados);
}
