package com.logistica.demo.sharedkernel.domain;

import java.util.Objects;

public record FiscalDimension(
        Long companyId,
        int year,
        int month,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        Long expenseClassifierId) {

    public FiscalDimension {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        Objects.requireNonNull(costCenterId, "costCenterId es obligatorio");
        Objects.requireNonNull(financingSourceId, "financingSourceId es obligatorio");
        Objects.requireNonNull(goalId, "goalId es obligatorio");
        Objects.requireNonNull(expenseClassifierId, "expenseClassifierId es obligatorio");
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("year debe tener cuatro digitos y ser igual o posterior a 2000");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month debe estar entre 1 y 12");
        }
    }
}

