package com.logistica.demo.logistica.compras.repository;

import com.logistica.demo.logistica.compras.domain.CotizacionProveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionProveedorRepository extends JpaRepository<CotizacionProveedor, Long> {

    Optional<CotizacionProveedor> findByProcesoIdAndProveedorId(Long procesoId, Long proveedorId);
}
