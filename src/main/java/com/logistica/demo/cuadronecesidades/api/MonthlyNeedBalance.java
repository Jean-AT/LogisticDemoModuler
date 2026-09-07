package com.logistica.demo.cuadronecesidades.api;

import java.math.BigDecimal;

public record MonthlyNeedBalance(
        int month,
        BigDecimal approvedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal availableQuantity) {
}

