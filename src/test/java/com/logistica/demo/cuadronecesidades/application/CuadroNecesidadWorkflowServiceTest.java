package com.logistica.demo.cuadronecesidades.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
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

        service.markReviewed(cuadro, window(TipoVentanaCuadroNecesidad.REVIEW, now.minusHours(1), now.plusHours(1)), now);

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

    private CuadroNecesidad planWithOneDetail() {
        CuadroNecesidad cuadro = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        cuadro.addDetail(detail(1));
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

    private List<ProgramacionMensualNecesidad> monthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProgramacionMensualNecesidad(month, quantity))
                .toList();
    }
}
