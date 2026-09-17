package com.logistica.demo.cuadronecesidades.dto;

import java.util.List;

public record CreateNeedsPlanRequest(
        Long companyId,
        int fiscalYear,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        String title,
        List<NeedLineRequest> details) {
}
