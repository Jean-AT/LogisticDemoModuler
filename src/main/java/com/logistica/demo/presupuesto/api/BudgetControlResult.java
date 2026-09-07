package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.Money;

public record BudgetControlResult(
        Long budgetControlId,
        BudgetControlStatus status,
        Money affectedAmount,
        Money availableAfter) {
}

