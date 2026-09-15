package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuadroNecesidadRepository extends JpaRepository<CuadroNecesidad, Long> {

    Optional<CuadroNecesidad> findByCompanyIdAndFiscalYearAndCostCenterIdAndFinancingSourceIdAndGoalId(
            Long companyId,
            int fiscalYear,
            Long costCenterId,
            Long financingSourceId,
            Long goalId);
}
