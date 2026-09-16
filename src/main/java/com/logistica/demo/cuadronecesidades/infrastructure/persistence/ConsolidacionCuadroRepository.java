package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsolidacionCuadroRepository extends JpaRepository<ConsolidacionCuadro, Long> {

    Optional<ConsolidacionCuadro> findByCompanyIdAndFiscalYearAndStatusIn(
            Long companyId,
            int fiscalYear,
            Collection<EstadoConsolidacionCuadro> statuses);
}
