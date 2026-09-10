package com.logistica.demo.platform.api;

import java.time.LocalDate;
import java.util.Objects;

public record FiscalPeriod(
        Long id,
        Long companyId,
        int fiscalYear,
        int month,
        FiscalPeriodStatus status,
        LocalDate startsOn,
        LocalDate endsOn) {

    public FiscalPeriod {
        Objects.requireNonNull(id, "id es obligatorio");
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        Objects.requireNonNull(status, "status es obligatorio");
        Objects.requireNonNull(startsOn, "startsOn es obligatorio");
        Objects.requireNonNull(endsOn, "endsOn es obligatorio");
        if (companyId <= 0 || id <= 0) {
            throw new IllegalArgumentException("Los identificadores deben ser positivos");
        }
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month debe estar entre 1 y 12");
        }
        if (endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("endsOn no puede ser anterior a startsOn");
        }
    }

    public boolean isOpen() {
        return status == FiscalPeriodStatus.OPEN;
    }
}
