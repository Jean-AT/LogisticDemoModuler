package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import java.util.Objects;

public record BudgetAllocation(FiscalDimension dimension, Money amount) {

    public BudgetAllocation {
        Objects.requireNonNull(dimension, "dimension es obligatoria");
        Objects.requireNonNull(amount, "amount es obligatorio");
    }
}

