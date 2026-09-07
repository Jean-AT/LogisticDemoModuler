package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;

public record BudgetAvailability(
        FiscalDimension dimension,
        Money assigned,
        Money precommitted,
        Money committed,
        Money available) {
}

