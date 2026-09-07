package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.util.Optional;

public interface BudgetAvailabilityQuery {

    Optional<BudgetAvailability> findAvailability(FiscalDimension dimension, Moneda currency);
}

