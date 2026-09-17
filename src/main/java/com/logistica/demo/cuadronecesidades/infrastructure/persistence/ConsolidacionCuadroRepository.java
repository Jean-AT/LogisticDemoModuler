package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsolidacionCuadroRepository extends JpaRepository<ConsolidacionCuadro, Long> {

    Optional<ConsolidacionCuadro> findByCompanyIdAndFiscalYearAndStatusIn(
            Long companyId,
            int fiscalYear,
            Collection<EstadoConsolidacionCuadro> statuses);

    Page<ConsolidacionCuadro> findByCompanyIdAndFiscalYear(Long companyId, int fiscalYear, Pageable pageable);

    List<ConsolidacionCuadro> findAllByCompanyIdAndFiscalYearAndStatusIn(
            Long companyId,
            int fiscalYear,
            Collection<EstadoConsolidacionCuadro> statuses);
}
