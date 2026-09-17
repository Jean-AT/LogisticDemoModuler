package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;

public record MonthlyNeedResponse(
        int month,
        BigDecimal requestedQuantity,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity,
        BigDecimal consumedQuantity) {
}
