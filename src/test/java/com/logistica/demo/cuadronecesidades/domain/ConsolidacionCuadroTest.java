package com.logistica.demo.cuadronecesidades.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ConsolidacionCuadroTest {

    @Test
    void shouldConsolidateOnlyReviewedPlansAndAggregateApprovedLines() {
        CuadroNecesidad plan = reviewedPlan();

        ConsolidacionCuadro consolidation = ConsolidacionCuadro.consolidate(
                1L,
                2026,
                List.of(plan),
                OffsetDateTime.parse("2026-03-01T10:00:00-05:00"));

        assertEquals(EstadoConsolidacionCuadro.CONSOLIDATED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.CONSOLIDATED, plan.getStatus());
        assertEquals(1, consolidation.getSources().size());
        assertEquals(1, consolidation.getLines().size());
        assertEquals(new BigDecimal("10"), consolidation.getLines().get(0).getApprovedQuantity());
        assertEquals(new BigDecimal("10000.00"), consolidation.getLines().get(0).getEstimatedTotal());
    }

    @Test
    void shouldRejectPlanThatWasNotReviewed() {
        CuadroNecesidad plan = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        plan.addDetail(detail(1, new BigDecimal("12"), new BigDecimal("1000.00")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConsolidacionCuadro.consolidate(
                        1L,
                        2026,
                        List.of(plan),
                        OffsetDateTime.parse("2026-03-01T10:00:00-05:00")));

        assertEquals("Solo se pueden consolidar cuadros en estado REVIEWED", exception.getMessage());
    }

    @Test
    void shouldReverseConsolidationBeforeTransfer() {
        CuadroNecesidad plan = reviewedPlan();
        ConsolidacionCuadro consolidation = ConsolidacionCuadro.consolidate(
                1L,
                2026,
                List.of(plan),
                OffsetDateTime.parse("2026-03-01T10:00:00-05:00"));

        consolidation.reverse(OffsetDateTime.parse("2026-03-02T10:00:00-05:00"));

        assertEquals(EstadoConsolidacionCuadro.REVERSED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.REVIEWED, plan.getStatus());
    }

    @Test
    void shouldRejectReversingTwice() {
        CuadroNecesidad plan = reviewedPlan();
        ConsolidacionCuadro consolidation = ConsolidacionCuadro.consolidate(
                1L,
                2026,
                List.of(plan),
                OffsetDateTime.parse("2026-03-01T10:00:00-05:00"));
        consolidation.reverse(OffsetDateTime.parse("2026-03-02T10:00:00-05:00"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consolidation.reverse(OffsetDateTime.parse("2026-03-03T10:00:00-05:00")));

        assertEquals("Solo se puede revertir una consolidacion no transferida", exception.getMessage());
    }

    @Test
    void shouldMarkConsolidationAndPlansAsTransferred() {
        CuadroNecesidad plan = reviewedPlan();
        ConsolidacionCuadro consolidation = ConsolidacionCuadro.consolidate(
                1L,
                2026,
                List.of(plan),
                OffsetDateTime.parse("2026-03-01T10:00:00-05:00"));

        consolidation.markTransferred(100L, 200L, OffsetDateTime.parse("2026-03-02T10:00:00-05:00"));

        assertEquals(EstadoConsolidacionCuadro.TRANSFERRED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.TRANSFERRED, plan.getStatus());
        assertEquals(100L, consolidation.getTransferId());
        assertEquals(200L, consolidation.getUnitBudgetExerciseId());
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consolidation.reverse(OffsetDateTime.parse("2026-03-03T10:00:00-05:00")));
        assertEquals("Solo se puede revertir una consolidacion no transferida", exception.getMessage());
    }

    private CuadroNecesidad reviewedPlan() {
        CuadroNecesidad plan = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        plan.addDetail(detail(1, new BigDecimal("12"), new BigDecimal("1000.00")));
        plan.addDetail(detail(2, new BigDecimal("12"), new BigDecimal("1000.00")));
        plan.submit(OffsetDateTime.parse("2026-01-10T10:00:00-05:00"));
        plan.applyReview(List.of(
                CuadroNecesidadDetalle.reviewed(
                        1,
                        new BigDecimal("6"),
                        new BigDecimal("6"),
                        reviewedMonthly(new BigDecimal("0.5000"))),
                CuadroNecesidadDetalle.reviewed(
                        2,
                        new BigDecimal("4"),
                        new BigDecimal("4"),
                        reviewedMonthly(new BigDecimal("0.3333"), new BigDecimal("0.0004")))));
        plan.markReviewed(OffsetDateTime.parse("2026-01-11T10:00:00-05:00"));
        return plan;
    }

    private CuadroNecesidadDetalle detail(int lineNumber, BigDecimal quantity, BigDecimal unitPrice) {
        return new CuadroNecesidadDetalle(
                lineNumber,
                100L,
                200L,
                300L,
                "ITEM-001",
                "Laptop",
                "UND",
                quantity,
                unitPrice,
                monthly(BigDecimal.ONE));
    }

    private List<ProgramacionMensualNecesidad> monthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProgramacionMensualNecesidad(month, quantity))
                .toList();
    }

    private List<ProgramacionMensualNecesidad> reviewedMonthly(BigDecimal approvedQuantity) {
        return reviewedMonthly(approvedQuantity, BigDecimal.ZERO);
    }

    private List<ProgramacionMensualNecesidad> reviewedMonthly(BigDecimal approvedQuantity, BigDecimal firstExtra) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> ProgramacionMensualNecesidad.reviewed(
                        month,
                        month == 1 ? approvedQuantity.add(firstExtra) : approvedQuantity,
                        month == 1 ? approvedQuantity.add(firstExtra) : approvedQuantity))
                .toList();
    }
}
