package com.logistica.demo.logistica.consultas.dto;

import java.util.List;

public record LogisticsTraceabilityResponse(
        Long requerimientoId,
        String requerimientoNumero,
        String requerimientoEstado,
        Long needsPlanId,
        Long needsLineId,
        Long budgetControlId,
        Long ordenCompraId,
        String ordenCompraNumero,
        String ordenCompraEstado,
        List<LogisticsTraceabilityEventResponse> events) {
}
