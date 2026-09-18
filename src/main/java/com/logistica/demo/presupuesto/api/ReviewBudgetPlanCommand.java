package com.logistica.demo.presupuesto.api;

import java.util.Objects;

public record ReviewBudgetPlanCommand(Long companyId, int fiscalYear, String reviewer, String notes) {

    public ReviewBudgetPlanCommand {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId es obligatorio");
        }
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        if (reviewer == null || reviewer.isBlank()) {
            throw new IllegalArgumentException("reviewer es obligatorio");
        }
        reviewer = reviewer.trim();
        notes = notes == null ? null : notes.trim();
    }
}
