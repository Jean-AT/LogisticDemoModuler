package com.logistica.demo.presupuesto.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistica.demo.presupuesto.api.ApproveBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetAvailability;
import com.logistica.demo.presupuesto.api.BudgetAvailabilityQuery;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.BudgetPlanResult;
import com.logistica.demo.presupuesto.api.BudgetPlanUseCase;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.presupuesto.api.GenerateBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.presupuesto.api.ReleaseBudgetCommand;
import com.logistica.demo.presupuesto.api.ReviewBudgetPlanCommand;
import com.logistica.demo.presupuesto.dto.BudgetAllocationRequest;
import com.logistica.demo.presupuesto.dto.BudgetAvailabilityResponse;
import com.logistica.demo.presupuesto.dto.BudgetControlRequest;
import com.logistica.demo.presupuesto.dto.BudgetDimensionRequest;
import com.logistica.demo.presupuesto.dto.BudgetDocumentRequest;
import com.logistica.demo.presupuesto.dto.BudgetPlanRequest;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class BudgetControllerTest {

    private RecordingPlanUseCase planUseCase;
    private RecordingControlUseCase controlUseCase;
    private BudgetController controller;

    @BeforeEach
    void setUp() {
        planUseCase = new RecordingPlanUseCase();
        controlUseCase = new RecordingControlUseCase();
        controller = new BudgetController(
                planUseCase,
                new FixedAvailabilityQuery(),
                controlUseCase,
                new CurrentUserService());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "aprobador",
                "demo123",
                List.of(new SimpleGrantedAuthority("ROLE_APROBADOR"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExposePlanAndAvailabilityOperations() {
        BudgetPlanResult generated = controller.generatePia(new BudgetPlanRequest(1L, 2026, null));
        BudgetPlanResult reviewed = controller.reviewPia(new BudgetPlanRequest(1L, 2026, "ok"));
        BudgetPlanResult approved = controller.approvePia(new BudgetPlanRequest(1L, 2026, null));
        BudgetAvailabilityResponse availability = controller.availability(1L, 2026, 1, 10L, 20L, 30L, 40L, "PEN");

        assertEquals(10L, generated.piaExerciseId());
        assertEquals(10L, reviewed.piaExerciseId());
        assertEquals(20L, approved.pimExerciseId());
        assertEquals("aprobador", planUseCase.generate.actor());
        assertEquals("ok", planUseCase.review.notes());
        assertEquals(new BigDecimal("750.00"), availability.available());
    }

    @Test
    void shouldExposeBudgetControlOperationsWithIdempotencyHeader() {
        BudgetControlRequest request = controlRequest("REQ-001", "250.00");

        BudgetControlResult precommit = controller.precommit(request, "precommit-key");
        BudgetControlResult commit = controller.commit(100L, request, "commit-key");
        BudgetControlResult release = controller.release(100L, request, "release-key");

        assertEquals(BudgetControlStatus.PRECOMMITTED, precommit.status());
        assertEquals(BudgetControlStatus.COMMITTED, commit.status());
        assertEquals(BudgetControlStatus.RELEASED, release.status());
        assertEquals("precommit-key", controlUseCase.precommit.idempotencyKey().value());
        assertEquals(100L, controlUseCase.commit.budgetControlId());
        assertEquals("release-key", controlUseCase.release.idempotencyKey().value());

        assertThrows(BadRequestException.class, () -> controller.precommit(request, null));
    }

    private BudgetControlRequest controlRequest(String number, String amount) {
        return new BudgetControlRequest(
                new BudgetDocumentRequest("LOGISTICA", "REQUERIMIENTO", 50L, number),
                List.of(new BudgetAllocationRequest(
                        new BudgetDimensionRequest(1L, 2026, 1, 10L, 20L, 30L, 40L),
                        new BigDecimal(amount),
                        "PEN")),
                "diferencia");
    }

    private static class RecordingPlanUseCase implements BudgetPlanUseCase {

        private GenerateBudgetPlanCommand generate;
        private ReviewBudgetPlanCommand review;

        @Override
        public BudgetPlanResult generatePia(GenerateBudgetPlanCommand command) {
            this.generate = command;
            return new BudgetPlanResult(10L, null, 2, false);
        }

        @Override
        public BudgetPlanResult reviewPia(ReviewBudgetPlanCommand command) {
            this.review = command;
            return new BudgetPlanResult(10L, null, 2, false);
        }

        @Override
        public BudgetPlanResult approvePiaAndCreateInitialPim(ApproveBudgetPlanCommand command) {
            return new BudgetPlanResult(10L, 20L, 2, false);
        }
    }

    private static class RecordingControlUseCase implements BudgetControlUseCase {

        private PrecommitBudgetCommand precommit;
        private CommitBudgetCommand commit;
        private ReleaseBudgetCommand release;

        @Override
        public BudgetControlResult precommit(PrecommitBudgetCommand command) {
            this.precommit = command;
            return result(BudgetControlStatus.PRECOMMITTED);
        }

        @Override
        public BudgetControlResult commit(CommitBudgetCommand command) {
            this.commit = command;
            return result(BudgetControlStatus.COMMITTED);
        }

        @Override
        public BudgetControlResult release(ReleaseBudgetCommand command) {
            this.release = command;
            return result(BudgetControlStatus.RELEASED);
        }

        private BudgetControlResult result(BudgetControlStatus status) {
            return new BudgetControlResult(
                    100L,
                    status,
                    new Money(new BigDecimal("250.00"), Moneda.PEN),
                    new Money(new BigDecimal("750.00"), Moneda.PEN));
        }
    }

    private static class FixedAvailabilityQuery implements BudgetAvailabilityQuery {

        @Override
        public Optional<BudgetAvailability> findAvailability(FiscalDimension dimension, Moneda currency) {
            return Optional.of(new BudgetAvailability(
                    dimension,
                    new Money(new BigDecimal("1000.00"), currency),
                    new Money(new BigDecimal("250.00"), currency),
                    new Money(BigDecimal.ZERO, currency),
                    new Money(new BigDecimal("750.00"), currency)));
        }
    }
}
