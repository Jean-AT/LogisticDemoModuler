package com.logistica.demo.cuadronecesidades.application;

import java.math.BigDecimal;

public record RevisionMensualCuadro(
        int month,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity) {
}
