package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuadroNecesidadRepository extends JpaRepository<CuadroNecesidad, Long> {

    Optional<CuadroNecesidad> findByCompanyIdAndFiscalYearAndCostCenterIdAndFinancingSourceIdAndGoalId(
            Long companyId,
            int fiscalYear,
            Long costCenterId,
            Long financingSourceId,
            Long goalId);

    Page<CuadroNecesidad> findByCompanyIdAndFiscalYear(Long companyId, int fiscalYear, Pageable pageable);

    Page<CuadroNecesidad> findByCompanyIdAndFiscalYearAndStatus(
            Long companyId,
            int fiscalYear,
            EstadoCuadroNecesidad status,
            Pageable pageable);

    List<CuadroNecesidad> findByCompanyIdAndFiscalYearAndStatus(
            Long companyId,
            int fiscalYear,
            EstadoCuadroNecesidad status);
}
