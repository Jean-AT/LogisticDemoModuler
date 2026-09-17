package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;

public record ReviewMonthlyNeedRequest(
        int month,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity) {
}
