package com.logistica.demo.platform.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.platform.api.AuditEventCommand;
import com.logistica.demo.platform.api.DefineFiscalPeriodCommand;
import com.logistica.demo.platform.api.DocumentSequencePort;
import com.logistica.demo.platform.api.FiscalPeriodAdministration;
import com.logistica.demo.platform.api.FiscalPeriodQuery;
import com.logistica.demo.platform.api.FiscalPeriodStatus;
import com.logistica.demo.platform.api.FunctionalAuditPort;
import com.logistica.demo.platform.api.PlatformCatalogQuery;
import com.logistica.demo.sharedkernel.event.DomainEvent;
import com.logistica.demo.sharedkernel.event.OutboxPort;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlatformOperationalAdaptersTest {

    @Autowired
    private FiscalPeriodAdministration fiscalPeriods;

    @Autowired
    private FiscalPeriodQuery fiscalPeriodQuery;

    @Autowired
    private DocumentSequencePort documentSequences;

    @Autowired
    private FunctionalAuditPort audit;

    @Autowired
    private OutboxPort outbox;

    @Autowired
    private IdempotencyPort idempotency;

    @Autowired
    private PlatformCatalogQuery catalogQuery;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDefineRequireAndCloseFiscalPeriods() {
        fiscalPeriods.definePeriod(new DefineFiscalPeriodCommand(
                1L,
                2026,
                9,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                FiscalPeriodStatus.OPEN,
                "admin"));

        var open = fiscalPeriodQuery.requireOpenPeriod(1L, LocalDate.of(2026, 9, 10));
        assertEquals(2026, open.fiscalYear());
        assertEquals(9, open.month());
        assertTrue(open.isOpen());

        fiscalPeriods.closePeriod(1L, 2026, 9, "admin");
        assertThrows(
                IllegalStateException.class,
                () -> fiscalPeriodQuery.requireOpenPeriod(1L, LocalDate.of(2026, 9, 10)));
    }

    @Test
    void shouldGenerateSequentialDocumentNumbersByCompanyYearAndType() {
        documentSequences.configureSequence(1L, 2026, "REQ", "REQ-2026", 41);

        var first = documentSequences.nextDocumentNumber(1L, 2026, "req");
        var second = documentSequences.nextDocumentNumber(1L, 2026, "REQ");

        assertEquals(42, first.value());
        assertEquals("REQ-2026-000042", first.formatted());
        assertEquals(43, second.value());
        assertEquals("REQ-2026-000043", second.formatted());
    }

    @Test
    void shouldRecordFunctionalAuditEvents() {
        var event = audit.record(new AuditEventCommand(
                1L,
                "admin",
                "ADMIN",
                "PERIOD.CLOSED",
                "FISCAL_PERIOD",
                "2026-09",
                "127.0.0.1",
                "trace-123",
                "{\"status\":\"CLOSED\"}"));

        assertNotNull(event.id());
        assertEquals("admin", event.actor());
        assertEquals("trace-123", event.traceId());
        assertEquals("{\"status\":\"CLOSED\"}", event.changesJson());
    }

    @Test
    void shouldAppendDomainEventsToOutbox() {
        UUID eventId = UUID.randomUUID();
        outbox.append(new TestDomainEvent(eventId, Instant.parse("2026-09-10T12:00:00Z"), 99L, "ok"));

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM platform.outbox_events
                WHERE id = ? AND aggregate_type = 'TEST' AND aggregate_id = '99'
                  AND event_type = 'TestDomainEvent'
                  AND payload LIKE '%"message":"ok"%'
                """,
                Integer.class,
                eventId);
        assertEquals(1, count);
    }

    @Test
    void shouldAcquireReplayAndRejectConflictingIdempotencyKeys() {
        Instant requestedAt = Instant.parse("2026-09-10T12:00:00Z");
        IdempotencyKey key = new IdempotencyKey("transfer-001");

        var acquired = idempotency.acquire("budget.transfer", key, "hash-a", requestedAt);
        assertEquals(IdempotencyClaimStatus.ACQUIRED, acquired.status());

        var inProgress = idempotency.acquire("budget.transfer", key, "hash-a", requestedAt);
        assertEquals(IdempotencyClaimStatus.IN_PROGRESS, inProgress.status());

        idempotency.complete(
                acquired.claimId(),
                new StoredResponse(201, "application/json", "{\"id\":10}"),
                requestedAt.plusSeconds(2));

        var replay = idempotency.acquire("budget.transfer", key, "hash-a", requestedAt.plusSeconds(3));
        assertEquals(IdempotencyClaimStatus.COMPLETED, replay.status());
        assertEquals(201, replay.storedResponse().statusCode());
        assertEquals("{\"id\":10}", replay.storedResponse().body());

        assertThrows(
                IllegalArgumentException.class,
                () -> idempotency.acquire("budget.transfer", key, "hash-b", requestedAt));
    }

    @Test
    void shouldExposeRepresentativePlatformCatalogReferences() {
        Long costCenterId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform.cost_centers WHERE code = 'CC-LOG'",
                Long.class);
        Long financingSourceId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform.financing_sources WHERE code = 'RO'",
                Long.class);
        Long goalId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform.goals WHERE code = 'META-002'",
                Long.class);
        Long classifierId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform.expense_classifiers WHERE code = '2.6.3.2.1.2'",
                Long.class);
        Long catalogItemId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform.catalog_items WHERE code = 'ITM-001'",
                Long.class);

        assertTrue(catalogQuery.findActiveCostCenter(1L, costCenterId).isPresent());
        assertTrue(catalogQuery.findActiveFinancingSource(financingSourceId).isPresent());
        assertTrue(catalogQuery.findActiveGoal(goalId).isPresent());
        assertTrue(catalogQuery.findActiveExpenseClassifier(classifierId).isPresent());
        assertEquals("ITM-001", catalogQuery.findActiveCatalogItem(catalogItemId).orElseThrow().code());
    }

    record TestDomainEvent(UUID eventId, Instant occurredAt, Long aggregate, String message)
            implements DomainEvent {

        @Override
        public String aggregateType() {
            return "TEST";
        }

        @Override
        public String aggregateId() {
            return aggregate.toString();
        }
    }
}
