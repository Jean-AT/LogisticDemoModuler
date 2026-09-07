package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import java.math.BigDecimal;

public record TransferredNeedMonth(
        FiscalDimension dimension,
        BigDecimal approvedQuantity,
        Money estimatedAmount) {
}

