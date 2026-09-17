package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;
import java.util.List;

public record NeedLineResponse(
        Long id,
        int lineNumber,
        Long catalogItemId,
        Long expenseClassifierId,
        Long unitOfMeasureId,
        String itemCode,
        String itemName,
        String unitCode,
        BigDecimal requestedQuantity,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedTotal,
        BigDecimal consumedQuantity,
        List<MonthlyNeedResponse> months) {
}
