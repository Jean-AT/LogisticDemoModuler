package com.logistica.demo.presupuesto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetFoundationTest {

    @Test
    void shouldOnlyApprovedExercisesContributeToAvailability() {
        EjercicioPresupuestal exercise = new EjercicioPresupuestal(1L, 2026, TipoEjercicioPresupuestal.PIA);

        assertFalse(exercise.contributesToAvailability());

        exercise.approve(OffsetDateTime.parse("2026-01-05T10:00:00Z"));

        assertEquals(EstadoEjercicioPresupuestal.APPROVED, exercise.getStatus());
        assertTrue(exercise.contributesToAvailability());

        exercise.close(OffsetDateTime.parse("2026-12-31T23:00:00Z"));

        assertEquals(EstadoEjercicioPresupuestal.CLOSED, exercise.getStatus());
        assertFalse(exercise.contributesToAvailability());
    }

    @Test
    void shouldRejectInvalidExerciseTransitions() {
        EjercicioPresupuestal exercise = new EjercicioPresupuestal(1L, 2026, TipoEjercicioPresupuestal.UNIDADES);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> exercise.close(OffsetDateTime.parse("2026-12-31T23:00:00Z")));

        assertEquals("Solo se puede cerrar un ejercicio APPROVED.", exception.getMessage());
    }

    @Test
    void shouldPreventAssignmentsAboveTheCeiling() {
        FiscalDimension dimension = dimension();
        TechoPresupuestal ceiling = new TechoPresupuestal(dimension, money("1000.00"));
        LineaPresupuestal line = new LineaPresupuestal(dimension, Moneda.PEN)
                .assign(ceiling, money("700.00"));

        assertEquals(0, new BigDecimal("700.00").compareTo(line.getAssigned().amount()));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> line.assign(ceiling, money("400.00")));

        assertEquals("La asignacion supera el techo presupuestal vigente.", exception.getMessage());
    }

    @Test
    void shouldExplainAvailabilityFromImmutableMovements() {
        FiscalDimension dimension = dimension();
        DocumentReference source = new DocumentReference("CUADRO", "NEEDS", 10L, "CN-2026-001");
        OffsetDateTime registeredAt = OffsetDateTime.parse("2026-01-10T12:00:00Z");

        LineaPresupuestal line = LineaPresupuestal.fromMovements(
                dimension,
                Moneda.PEN,
                List.of(
                        movement(TipoMovimientoPresupuestal.ASSIGNMENT, dimension, "1000.00", source, registeredAt),
                        movement(TipoMovimientoPresupuestal.PRECOMMITMENT, dimension, "250.00", source, registeredAt),
                        movement(TipoMovimientoPresupuestal.COMMITMENT, dimension, "300.00", source, registeredAt),
                        movement(TipoMovimientoPresupuestal.PRECOMMITMENT_RELEASE, dimension, "50.00", source, registeredAt)));

        assertEquals(0, new BigDecimal("1000.00").compareTo(line.getAssigned().amount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(line.getPrecommitted().amount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(line.getCommitted().amount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(line.available().amount()));
        assertEquals(line.available(), line.toAvailability().available());
    }

    @Test
    void shouldRejectMovementsThatProduceNegativeAvailability() {
        FiscalDimension dimension = dimension();
        DocumentReference source = new DocumentReference("LOGISTICA", "REQ", 50L, "REQ-001");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> LineaPresupuestal.fromMovements(
                        dimension,
                        Moneda.PEN,
                        List.of(
                                movement(TipoMovimientoPresupuestal.ASSIGNMENT, dimension, "100.00", source, OffsetDateTime.now()),
                                movement(TipoMovimientoPresupuestal.PRECOMMITMENT, dimension, "125.00", source, OffsetDateTime.now()))));

        assertEquals("La disponibilidad presupuestal no puede ser negativa.", exception.getMessage());
    }

    private MovimientoPresupuestal movement(
            TipoMovimientoPresupuestal type,
            FiscalDimension dimension,
            String amount,
            DocumentReference source,
            OffsetDateTime registeredAt) {
        return new MovimientoPresupuestal(type, dimension, money(amount), source, registeredAt, "admin");
    }

    private FiscalDimension dimension() {
        return new FiscalDimension(1L, 2026, 1, 10L, 20L, 30L, 40L);
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), Moneda.PEN);
    }
}
