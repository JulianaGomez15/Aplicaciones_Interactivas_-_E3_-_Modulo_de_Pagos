package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {
    MedioPago findByCodigoMedioPago(UUID codigoMedioPago);
}
