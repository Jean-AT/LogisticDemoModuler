package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;

public record ConsolidationLineResponse(
        Long id,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        Long expenseClassifierId,
        Long catalogItemId,
        Long unitOfMeasureId,
        String itemCode,
        String itemName,
        String unitCode,
        BigDecimal approvedQuantity,
        BigDecimal estimatedTotal) {
}
