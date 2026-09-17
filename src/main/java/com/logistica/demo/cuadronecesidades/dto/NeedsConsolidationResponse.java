package com.logistica.demo.cuadronecesidades.dto;

import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import java.time.OffsetDateTime;
import java.util.List;

public record NeedsConsolidationResponse(
        Long id,
        Long companyId,
        int fiscalYear,
        EstadoConsolidacionCuadro status,
        OffsetDateTime consolidatedAt,
        OffsetDateTime reversedAt,
        OffsetDateTime transferredAt,
        Long transferId,
        Long unitBudgetExerciseId,
        List<ConsolidationSourceResponse> sources,
        List<ConsolidationLineResponse> lines) {
}
