package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.domain.TipoVentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.VentanaCuadroNecesidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentanaCuadroNecesidadRepository extends JpaRepository<VentanaCuadroNecesidad, Long> {

    Optional<VentanaCuadroNecesidad> findByCompanyIdAndFiscalYearAndWindowTypeAndActiveTrue(
            Long companyId,
            int fiscalYear,
            TipoVentanaCuadroNecesidad windowType);
}
