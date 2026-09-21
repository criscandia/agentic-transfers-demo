package com.demo.banking.repository;

import com.demo.banking.model.Transferencia;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Query("select coalesce(sum(t.monto), 0) from Transferencia t "
            + "where t.cuentaOrigenId = :cuentaOrigenId and t.fecha >= :desde")
    BigDecimal sumaTransferidaDesde(@Param("cuentaOrigenId") String cuentaOrigenId, @Param("desde") Instant desde);
}
