package com.logistica.demo.cuadronecesidades.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.ProgramacionMensualNecesidad;
import com.logistica.demo.cuadronecesidades.domain.TipoVentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.VentanaCuadroNecesidad;
import com.logistica.demo.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CuadroNecesidadWorkflowServiceTest {

    private final CuadroNecesidadWorkflowService service = new CuadroNecesidadWorkflowService();

    @Test
    void shouldSubmitOnlyWhenRegistrationWindowIsOpen() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-10T10:00:00-05:00");
        CuadroNecesidad cuadro = planWithOneDetail();

        service.submit(cuadro, window(TipoVentanaCuadroNecesidad.REGISTRATION, now.minusDays(1), now.plusDays(1)), now);

        assertEquals(EstadoCuadroNecesidad.SUBMITTED, cuadro.getStatus());
    }

    @Test
    void shouldRejectSubmitOutsideRegistrationWindow() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-10T10:00:00-05:00");
        CuadroNecesidad cuadro = planWithOneDetail();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.submit(
                        cuadro,
                        window(TipoVentanaCuadroNecesidad.REGISTRATION, now.minusDays(3), now.minusDays(1)),
                        now));

        assertEquals("La ventana de REGISTRATION no esta abierta.", exception.getMessage());
    }

    @Test
    void shouldReviewOnlyWhenReviewWindowIsOpen() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-10T10:00:00-05:00");
        CuadroNecesidad cuadro = planWithOneDetail();
        service.submit(cuadro, window(TipoVentanaCuadroNecesidad.REGISTRATION, now.minusDays(1), now.plusDays(1)), now);

        service.markReviewed(
                cuadro,
                revisions(1, BigDecimal.ONE, BigDecimal.ONE),
                window(TipoVentanaCuadroNecesidad.REVIEW, now.minusHours(1), now.plusHours(1)),
                now);

        assertEquals(EstadoCuadroNecesidad.REVIEWED, cuadro.getStatus());
    }

    @Test
    void shouldRejectReviewWithWrongWindowType() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-10T10:00:00-05:00");
        CuadroNecesidad cuadro = planWithOneDetail();
        service.submit(cuadro, window(TipoVentanaCuadroNecesidad.REGISTRATION, now.minusDays(1), now.plusDays(1)), now);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.reject(
                        cuadro,
                        window(TipoVentanaCuadroNecesidad.CONSOLIDATION, now.minusHours(1), now.plusHours(1)),
                        now));

        assertEquals("La ventana de REVIEW no esta abierta.", exception.getMessage());
    }

    @Test
    void shouldRejectReviewWhenRevisionDoesNotCoverAllLines() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-10T10:00:00-05:00");
        CuadroNecesidad cuadro = planWithOneDetail();
        service.submit(cuadro, window(TipoVentanaCuadroNecesidad.REGISTRATION, now.minusDays(1), now.plusDays(1)), now);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.markReviewed(
                        cuadro,
                        List.of(),
                        window(TipoVentanaCuadroNecesidad.REVIEW, now.minusHours(1), now.plusHours(1)),
                        now));

        assertEquals("La revision debe incluir todas las lineas del cuadro", exception.getMessage());
    }

    @Test
    void shouldConsolidateAndReverseOnlyWhenConsolidationWindowIsOpen() {
        OffsetDateTime now = OffsetDateTime.parse("2026-03-10T10:00:00-05:00");
        CuadroNecesidad cuadro = reviewedPlan(now.minusDays(10));

        var consolidation = service.consolidate(
                1L,
                2026,
                List.of(cuadro),
                window(TipoVentanaCuadroNecesidad.CONSOLIDATION, now.minusDays(1), now.plusDays(1)),
                now);

        assertEquals(EstadoConsolidacionCuadro.CONSOLIDATED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.CONSOLIDATED, cuadro.getStatus());

        service.reverseConsolidation(
                consolidation,
                window(TipoVentanaCuadroNecesidad.CONSOLIDATION, now.minusDays(1), now.plusDays(1)),
                now.plusHours(1));

        assertEquals(EstadoConsolidacionCuadro.REVERSED, consolidation.getStatus());
        assertEquals(EstadoCuadroNecesidad.REVIEWED, cuadro.getStatus());
    }

    private CuadroNecesidad planWithOneDetail() {
        CuadroNecesidad cuadro = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        cuadro.addDetail(detail(1));
        return cuadro;
    }

    private CuadroNecesidad reviewedPlan(OffsetDateTime baseDate) {
        CuadroNecesidad cuadro = planWithOneDetail();
        service.submit(cuadro, window(TipoVentanaCuadroNecesidad.REGISTRATION, baseDate, baseDate.plusDays(1)), baseDate.plusHours(1));
        service.markReviewed(
                cuadro,
                revisions(1, BigDecimal.ONE, BigDecimal.ONE),
                window(TipoVentanaCuadroNecesidad.REVIEW, baseDate.plusDays(1), baseDate.plusDays(2)),
                baseDate.plusDays(1).plusHours(1));
        return cuadro;
    }

    private VentanaCuadroNecesidad window(
            TipoVentanaCuadroNecesidad type,
            OffsetDateTime opensAt,
            OffsetDateTime closesAt) {
        return new VentanaCuadroNecesidad(1L, 2026, type, opensAt, closesAt);
    }

    private CuadroNecesidadDetalle detail(int lineNumber) {
        return new CuadroNecesidadDetalle(
                lineNumber,
                100L,
                200L,
                300L,
                "ITEM-001",
                "Laptop",
                "UND",
                new BigDecimal("12"),
                new BigDecimal("1200.00"),
                monthly(BigDecimal.ONE));
    }

    private List<RevisionLineaCuadro> revisions(
            int lineNumber,
            BigDecimal monthlyReviewedQuantity,
            BigDecimal monthlyApprovedQuantity) {
        return List.of(new RevisionLineaCuadro(
                lineNumber,
                monthlyReviewedQuantity.multiply(new BigDecimal("12")),
                monthlyApprovedQuantity.multiply(new BigDecimal("12")),
                IntStream.rangeClosed(1, 12)
                        .mapToObj(month -> new RevisionMensualCuadro(
                                month,
                                monthlyReviewedQuantity,
                                monthlyApprovedQuantity))
                        .toList()));
    }

    private List<ProgramacionMensualNecesidad> monthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProgramacionMensualNecesidad(month, quantity))
                .toList();
    }
}
