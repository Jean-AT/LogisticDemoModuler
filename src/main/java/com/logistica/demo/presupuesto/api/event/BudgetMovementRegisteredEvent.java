package com.logistica.demo.presupuesto.api.event;

import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record BudgetMovementRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        Long budgetControlId,
        BudgetControlStatus status,
        DocumentReference source,
        Money amount) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "BUDGET_CONTROL";
    }

    @Override
    public String aggregateId() {
        return budgetControlId.toString();
    }
}
