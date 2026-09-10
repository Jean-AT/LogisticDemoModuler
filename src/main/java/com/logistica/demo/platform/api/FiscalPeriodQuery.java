package com.logistica.demo.platform.api;

import java.time.LocalDate;
import java.util.Optional;

public interface FiscalPeriodQuery {

    Optional<FiscalPeriod> findByCompanyAndDate(Long companyId, LocalDate date);

    Optional<FiscalPeriod> findByCompanyYearAndMonth(Long companyId, int fiscalYear, int month);

    FiscalPeriod requireOpenPeriod(Long companyId, LocalDate date);
}
