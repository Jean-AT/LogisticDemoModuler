package com.logistica.demo.presupuesto.domain;

import com.logistica.demo.presupuesto.api.BudgetAvailability;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

public class LineaPresupuestal {

    private final FiscalDimension dimension;
    private final Moneda currency;
    private final Money assigned;
    private final Money precommitted;
    private final Money committed;

    public LineaPresupuestal(FiscalDimension dimension, Moneda currency) {
        this(dimension, currency, zero(currency), zero(currency), zero(currency));
    }

    private LineaPresupuestal(
            FiscalDimension dimension,
            Moneda currency,
            Money assigned,
            Money precommitted,
            Money committed) {
        this.dimension = Objects.requireNonNull(dimension, "dimension es obligatoria");
        this.currency = Objects.requireNonNull(currency, "currency es obligatoria");
        this.assigned = requireCurrency(assigned, currency, "assigned");
        this.precommitted = requireCurrency(precommitted, currency, "precommitted");
        this.committed = requireCurrency(committed, currency, "committed");
        requireNonNegative(this.assigned, "assigned");
        requireNonNegative(this.precommitted, "precommitted");
        requireNonNegative(this.committed, "committed");
        if (available().amount().signum() < 0) {
            throw new IllegalStateException("La disponibilidad presupuestal no puede ser negativa.");
        }
    }

    public LineaPresupuestal assign(TechoPresupuestal ceiling, Money amount) {
        Objects.requireNonNull(ceiling, "ceiling es obligatorio");
        requireSameDimension(ceiling.dimension());
        requirePositive(requireCurrency(amount, currency, "amount"), "amount");
        Money nextAssigned = assigned.add(amount);
        if (!ceiling.covers(nextAssigned)) {
            throw new IllegalStateException("La asignacion supera el techo presupuestal vigente.");
        }
        return new LineaPresupuestal(dimension, currency, nextAssigned, precommitted, committed);
    }

    public LineaPresupuestal apply(MovimientoPresupuestal movement) {
        Objects.requireNonNull(movement, "movement es obligatorio");
        requireSameDimension(movement.dimension());
        Money amount = requireCurrency(movement.amount(), currency, "movement.amount");
        return switch (movement.type()) {
            case ASSIGNMENT -> new LineaPresupuestal(dimension, currency, assigned.add(amount), precommitted, committed);
            case PRECOMMITMENT -> new LineaPresupuestal(dimension, currency, assigned, precommitted.add(amount), committed);
            case PRECOMMITMENT_RELEASE -> new LineaPresupuestal(dimension, currency, assigned, precommitted.subtract(amount), committed);
            case COMMITMENT -> new LineaPresupuestal(dimension, currency, assigned, precommitted, committed.add(amount));
            case COMMITMENT_RELEASE -> new LineaPresupuestal(dimension, currency, assigned, precommitted, committed.subtract(amount));
        };
    }

    public static LineaPresupuestal fromMovements(
            FiscalDimension dimension,
            Moneda currency,
            Collection<MovimientoPresupuestal> movements) {
        LineaPresupuestal line = new LineaPresupuestal(dimension, currency);
        for (MovimientoPresupuestal movement : movements) {
            line = line.apply(movement);
        }
        return line;
    }

    public BudgetAvailability toAvailability() {
        return new BudgetAvailability(dimension, assigned, precommitted, committed, available());
    }

    public Money available() {
        return assigned.subtract(precommitted).subtract(committed);
    }

    public FiscalDimension getDimension() {
        return dimension;
    }

    public Moneda getCurrency() {
        return currency;
    }

    public Money getAssigned() {
        return assigned;
    }

    public Money getPrecommitted() {
        return precommitted;
    }

    public Money getCommitted() {
        return committed;
    }

    private void requireSameDimension(FiscalDimension other) {
        if (!dimension.equals(other)) {
            throw new IllegalArgumentException("La dimension presupuestal no coincide.");
        }
    }

    private static Money requireCurrency(Money money, Moneda currency, String field) {
        Objects.requireNonNull(money, field + " es obligatorio");
        if (money.currency() != currency) {
            throw new IllegalArgumentException(field + " debe estar expresado en " + currency);
        }
        return money;
    }

    private static void requirePositive(Money money, String field) {
        if (money.amount().signum() <= 0) {
            throw new IllegalArgumentException(field + " debe ser positivo");
        }
    }

    private static void requireNonNegative(Money money, String field) {
        if (money.amount().signum() < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
    }

    private static Money zero(Moneda currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
}
