package com.logistica.demo.cuadronecesidades.dto;

import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import java.time.OffsetDateTime;
import java.util.List;

public record NeedsPlanResponse(
        Long id,
        Long companyId,
        int fiscalYear,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        EstadoCuadroNecesidad status,
        String title,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        OffsetDateTime consolidatedAt,
        List<NeedLineResponse> details) {
}
