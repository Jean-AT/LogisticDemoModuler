package com.logistica.demo.presupuesto.dto;

import java.math.BigDecimal;

public record BudgetAllocationRequest(
        BudgetDimensionRequest dimension,
        BigDecimal amount,
        String currency) {
}
