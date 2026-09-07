package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.util.List;

public record CommitBudgetCommand(
        Long budgetControlId,
        DocumentReference source,
        List<BudgetAllocation> allocations,
        IdempotencyKey idempotencyKey,
        String actor) {

    public CommitBudgetCommand {
        allocations = List.copyOf(allocations);
    }
}

