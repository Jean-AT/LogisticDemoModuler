package com.logistica.demo.cuadronecesidades.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CuadroNecesidadTest {

    @Test
    void shouldCreateDraftPlanWithTwelveMonthLineSchedule() {
        CuadroNecesidad cuadro = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        CuadroNecesidadDetalle detalle = detail(1, monthly(BigDecimal.ONE));

        cuadro.addDetail(detalle);

        assertEquals(EstadoCuadroNecesidad.DRAFT, cuadro.getStatus());
        assertEquals(1, cuadro.getDetails().size());
        assertEquals(1L, detalle.getCompanyId());
        assertEquals(new BigDecimal("12"), detalle.getRequestedQuantity());
        assertEquals(12, detalle.getMonthlyNeeds().size());
    }

    @Test
    void shouldRejectLineWhenMonthlyScheduleDoesNotMatchAnnualQuantity() {
        List<ProgramacionMensualNecesidad> months = new ArrayList<>(monthly(BigDecimal.ONE));
        months.set(0, new ProgramacionMensualNecesidad(1, BigDecimal.ZERO));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> detail(1, months));

        assertEquals("La suma mensual debe coincidir con la cantidad anual solicitada", exception.getMessage());
    }

    @Test
    void shouldRejectRepeatedLineNumbers() {
        CuadroNecesidad cuadro = new CuadroNecesidad(1L, 2026, 10L, 20L, 30L, "Plan anual");
        cuadro.addDetail(detail(1, monthly(BigDecimal.ONE)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cuadro.addDetail(detail(1, monthly(BigDecimal.ONE))));

        assertEquals("Los numeros de linea no pueden repetirse", exception.getMessage());
    }

    private CuadroNecesidadDetalle detail(int lineNumber, List<ProgramacionMensualNecesidad> months) {
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
                months);
    }

    private List<ProgramacionMensualNecesidad> monthly(BigDecimal quantity) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProgramacionMensualNecesidad(month, quantity))
                .toList();
    }
}
