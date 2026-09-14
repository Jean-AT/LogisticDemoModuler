package com.logistica.demo.platform.dto;

import com.logistica.demo.platform.api.FiscalPeriodStatus;
import java.time.LocalDate;

public record DefineFiscalPeriodRequest(
        Long companyId,
        int fiscalYear,
        int month,
        LocalDate startsOn,
        LocalDate endsOn,
        FiscalPeriodStatus status) {
}
