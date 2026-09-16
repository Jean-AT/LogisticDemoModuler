package com.logistica.demo.cuadronecesidades.api;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import java.math.BigDecimal;

public record NeedsBudgetTransferMonth(
        FiscalDimension dimension,
        BigDecimal approvedQuantity,
        Money estimatedAmount) {
}
