package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.util.List;

public record ReleaseBudgetCommand(
        Long budgetControlId,
        DocumentReference source,
        List<BudgetAllocation> allocations,
        String reason,
        IdempotencyKey idempotencyKey,
        String actor) {

    public ReleaseBudgetCommand {
        allocations = List.copyOf(allocations);
    }
}

