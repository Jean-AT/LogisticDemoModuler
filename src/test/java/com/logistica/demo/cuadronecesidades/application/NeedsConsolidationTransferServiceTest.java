package com.logistica.demo.cuadronecesidades.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferPort;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.ProgramacionMensualNecesidad;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.ConsolidacionCuadroRepository;
import com.logistica.demo.sharedkernel.event.DomainEvent;
import com.logistica.demo.sharedkernel.event.OutboxPort;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaim;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

class NeedsConsolidationTransferServiceTest {

    @Test
    void shouldTransferConsolidationOnceAndReplayIdempotentRetry() {
        ConsolidacionCuadro consolidation = persistedConsolidation();
        ConsolidacionCuadroRepository repository = mock(ConsolidacionCuadroRepository.class);
        when(repository.findById(1000L)).thenReturn(Optional.of(consolidation));
        when(repository.save(any(ConsolidacionCuadro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NeedsBudgetTransferPort budget = mock(NeedsBudgetTransferPort.class);
        when(budget.transfer(any())).thenReturn(new NeedsBudgetTransferResult(900L, 901L, 1, false));
        RecordingIdempotency idempotency = new RecordingIdempotency();
        RecordingOutbox outbox = new RecordingOutbox();

        NeedsConsolidationTransferService service = new NeedsConsolidationTransferService(
                repository,
                provider(budget),
                idempotency,
                outbox);

        NeedsBudgetTransferResult first = service.transfer(
                1000L,
                new IdempotencyKey("transfer-cn-2026"),
                "admin",
                OffsetDateTime.parse("2026-03-15T10:00:00-05:00"));
        NeedsBudgetTransferResult replay = service.transfer(
                1000L,
                new IdempotencyKey("transfer-cn-2026"),
                "admin",
                OffsetDateTime.parse("2026-03-15T10:01:00-05:00"));

        assertEquals(new NeedsBudgetTransferResult(900L, 901L, 1, false), first);
        assertEquals(new NeedsBudgetTransferResult(900L, 901L, 1, true), replay);
        assertEquals(EstadoConsolidacionCuadro.TRANSFERRED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.TRANSFERRED, consolidation.getSources().get(0).getPlan().getStatus());
        assertEquals(1, outbox.events.size());
        verify(budget).transfer(any());
    }

    private ConsolidacionCuadro persistedConsolidation() {
        CuadroNecesidad plan = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        ReflectionTestUtils.setField(plan, "id", 100L);
        CuadroNecesidadDetalle detail = new CuadroNecesidadDetalle(
                1,
                200L,
                300L,
                400L,
                "ITEM-001",
                "Laptop",
                "UND",
                new BigDecimal("12.0000"),
                new BigDecimal("100.00"),
                monthly(BigDecimal.ONE));
        plan.addDetail(detail);
        ReflectionTestUtils.setField(detail, "id", 101L);
        plan.submit(OffsetDateTime.parse("2026-01-10T10:00:00-05:00"));
        plan.applyReview(List.of(CuadroNecesidadDetalle.reviewed(
                1,
                new BigDecimal("12.0000"),
                new BigDecimal("12.0000"),
                reviewedMonthly(BigDecimal.ONE))));
        plan.markReviewed(OffsetDateTime.parse("2026-01-11T10:00:00-05:00"));
        ConsolidacionCuadro consolidation = ConsolidacionCuadro.consolidate(
                1L,
                2026,
                List.of(plan),
                OffsetDateTime.parse("2026-03-01T10:00:00-05:00"));
        ReflectionTestUtils.setField(consolidation, "id", 1000L);
        return consolidation;
    }

    private List<ProgramacionMensualNecesidad> monthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProgramacionMensualNecesidad(month, quantity))
                .toList();
    }

    private List<ProgramacionMensualNecesidad> reviewedMonthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> ProgramacionMensualNecesidad.reviewed(month, quantity, quantity))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<NeedsBudgetTransferPort> provider(NeedsBudgetTransferPort budget) {
        ObjectProvider<NeedsBudgetTransferPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(budget);
        return provider;
    }

    private class RecordingIdempotency implements IdempotencyPort {

        private final UUID claimId = UUID.randomUUID();
        private StoredResponse response;

        @Override
        public IdempotencyClaim acquire(
                String operation,
                IdempotencyKey key,
                String requestHash,
                Instant requestedAt) {
            if (response != null) {
                return new IdempotencyClaim(IdempotencyClaimStatus.COMPLETED, claimId, response);
            }
            return new IdempotencyClaim(IdempotencyClaimStatus.ACQUIRED, claimId, null);
        }

        @Override
        public void complete(UUID claimId, StoredResponse response, Instant completedAt) {
            this.response = response;
        }

        @Override
        public void release(UUID claimId) {
            response = null;
        }
    }

    private static class RecordingOutbox implements OutboxPort {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void append(DomainEvent event) {
            events.add(event);
        }
    }
}
