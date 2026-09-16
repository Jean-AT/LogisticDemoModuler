package com.logistica.demo.cuadronecesidades.application;

import java.math.BigDecimal;
import java.util.List;

public record RevisionLineaCuadro(
        int lineNumber,
        BigDecimal reviewedQuantity,
        BigDecimal approvedQuantity,
        List<RevisionMensualCuadro> months) {
}
