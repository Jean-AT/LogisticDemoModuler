package com.logistica.demo.platform.api;

public interface FiscalPeriodAdministration {

    FiscalPeriod definePeriod(DefineFiscalPeriodCommand command);

    FiscalPeriod openPeriod(Long companyId, int fiscalYear, int month, String actor);

    FiscalPeriod closePeriod(Long companyId, int fiscalYear, int month, String actor);
}
