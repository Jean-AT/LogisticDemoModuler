package com.logistica.demo.logistica.compras.repository;

import com.logistica.demo.logistica.compras.domain.ProcesoCotizacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcesoCotizacionRepository extends JpaRepository<ProcesoCotizacion, Long> {

    Optional<ProcesoCotizacion> findByRequerimientoId(Long requerimientoId);

    boolean existsByRequerimientoId(Long requerimientoId);
}
