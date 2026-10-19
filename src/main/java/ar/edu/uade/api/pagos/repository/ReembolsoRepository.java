package ar.edu.uade.api.pagos.repository;

import ar.edu.uade.api.pagos.model.Pago;
import ar.edu.uade.api.pagos.model.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ReembolsoRepository extends JpaRepository<Reembolso, Long> {

    Reembolso findByClaveControl(String claveControl);

    // Total ya reembolsado de un pago (RN-07).
    @Query("select coalesce(sum(r.monto), 0) from Reembolso r where r.pago = :pago")
    BigDecimal sumarMontosPorPago(@Param("pago") Pago pago);
}
