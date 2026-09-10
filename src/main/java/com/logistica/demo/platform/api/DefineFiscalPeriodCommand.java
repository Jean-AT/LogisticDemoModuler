package com.logistica.demo.platform.api;

import java.time.LocalDate;
import java.util.Objects;

public record DefineFiscalPeriodCommand(
        Long companyId,
        int fiscalYear,
        int month,
        LocalDate startsOn,
        LocalDate endsOn,
        FiscalPeriodStatus status,
        String actor) {

    public DefineFiscalPeriodCommand {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        Objects.requireNonNull(startsOn, "startsOn es obligatorio");
        Objects.requireNonNull(endsOn, "endsOn es obligatorio");
        Objects.requireNonNull(status, "status es obligatorio");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
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
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        actor = actor.trim();
    }
}
