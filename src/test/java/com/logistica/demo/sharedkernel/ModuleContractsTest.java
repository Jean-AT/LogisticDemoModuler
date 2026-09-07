package com.logistica.demo.sharedkernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistica.demo.cuadronecesidades.api.event.NeedsConsolidationTransferredEvent;
import com.logistica.demo.presupuesto.api.TransferNeedsCommand;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaim;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModuleContractsTest {

    @Test
    void transferCommandKeepsAnImmutableSnapshotOfLines() {
        List<com.logistica.demo.presupuesto.api.TransferredNeedLine> lines = new ArrayList<>();
        TransferNeedsCommand command = new TransferNeedsCommand(
                1L,
                2L,
                2026,
                lines,
                new IdempotencyKey(" transfer-1 "),
                "usuario");

        lines.clear();
        assertEquals(0, command.lines().size());
        assertEquals("transfer-1", command.idempotencyKey().value());
        assertThrows(UnsupportedOperationException.class, () -> command.lines().clear());
    }

    @Test
    void domainEventProvidesStableEnvelopeMetadata() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        NeedsConsolidationTransferredEvent event = new NeedsConsolidationTransferredEvent(
                eventId,
                occurredAt,
                10L,
                20L,
                30L,
                2026);

        assertEquals(eventId, event.eventId());
        assertEquals("NeedsConsolidationTransferredEvent", event.eventType());
        assertEquals("NEEDS_CONSOLIDATION", event.aggregateType());
        assertEquals("10", event.aggregateId());
    }

    @Test
    void idempotencyClaimRequiresDataForItsStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IdempotencyClaim(IdempotencyClaimStatus.ACQUIRED, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IdempotencyClaim(IdempotencyClaimStatus.COMPLETED, UUID.randomUUID(), null));
    }
}
