package com.logistica.demo.presupuesto.api;

import java.util.Objects;

public record ApproveBudgetPlanCommand(Long companyId, int fiscalYear, String actor) {

    public ApproveBudgetPlanCommand {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId es obligatorio");
        }
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        actor = actor.trim();
    }
}
