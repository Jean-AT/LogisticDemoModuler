package com.logistica.demo.cuadronecesidades.dto;

import java.math.BigDecimal;

public record MonthlyNeedRequest(
        int month,
        BigDecimal requestedQuantity) {
}
