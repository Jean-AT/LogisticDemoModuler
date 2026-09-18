package com.logistica.demo.presupuesto.infrastructure.persistence;

import com.logistica.demo.presupuesto.api.BudgetAvailability;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;

record BudgetLineState(
        long budgetLineId,
        FiscalDimension dimension,
        Moneda currency,
        BigDecimal assigned,
        BigDecimal precommitted,
        BigDecimal committed) {

    BigDecimal availableAmount() {
        return assigned.subtract(precommitted).subtract(committed);
    }

    BudgetAvailability toAvailability() {
        return new BudgetAvailability(
                dimension,
                new Money(assigned, currency),
                new Money(precommitted, currency),
                new Money(committed, currency),
                new Money(availableAmount(), currency));
    }
}
