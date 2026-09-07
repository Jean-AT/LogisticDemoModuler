package com.logistica.demo.cuadronecesidades.api.event;

import com.logistica.demo.sharedkernel.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record NeedsConsolidationTransferredEvent(
        UUID eventId,
        Instant occurredAt,
        Long consolidationId,
        Long transferId,
        Long companyId,
        int fiscalYear) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "NEEDS_CONSOLIDATION";
    }

    @Override
    public String aggregateId() {
        return consolidationId.toString();
    }
}
