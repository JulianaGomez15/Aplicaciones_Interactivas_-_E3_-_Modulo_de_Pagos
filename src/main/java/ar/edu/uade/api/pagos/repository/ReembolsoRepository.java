package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReembolsoRepository extends JpaRepository<Reembolso, Long> {
    Reembolso findByCodigo(UUID codigo);

    List<Reembolso> findByPagoCodigoOrderByFechaAscIdAsc(UUID codigoPago);
}
