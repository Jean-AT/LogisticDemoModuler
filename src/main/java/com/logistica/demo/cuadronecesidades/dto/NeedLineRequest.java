package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;
import java.util.List;

public record NeedLineRequest(
        int lineNumber,
        Long catalogItemId,
        Long expenseClassifierId,
        Long unitOfMeasureId,
        String itemCode,
        String itemName,
        String unitCode,
        BigDecimal requestedQuantity,
        BigDecimal estimatedUnitPrice,
        List<MonthlyNeedRequest> months) {
}
