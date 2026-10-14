package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {
    MedioPago findByCodigo(UUID codigo);

    List<MedioPago> findByCodigoUsuarioOrderByFechaCreacionDescIdDesc(UUID codigoUsuario);
}
