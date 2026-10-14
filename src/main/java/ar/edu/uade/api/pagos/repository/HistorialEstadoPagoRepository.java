package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.HistorialEstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialEstadoPagoRepository extends JpaRepository<HistorialEstadoPago, Long> {
    List<HistorialEstadoPago> findByPagoCodigoOrderByFechaAscIdAsc(UUID codigoPago);
}
