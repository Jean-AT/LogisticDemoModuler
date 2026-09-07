package com.logistica.demo.cuadronecesidades.api;

import java.math.BigDecimal;
import java.util.List;

public record NeedsLineBalance(
        Long needsPlanId,
        Long needsLineId,
        Long companyId,
        int fiscalYear,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        Long expenseClassifierId,
        Long catalogItemId,
        BigDecimal approvedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal availableQuantity,
        List<MonthlyNeedBalance> months) {
}

