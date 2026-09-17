package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReviewNeedLineRequest(
        int lineNumber,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity,
        List<ReviewMonthlyNeedRequest> months) {
}
