package com.logistica.demo.cuadronecesidades.dto;

public record ConsolidationSourceResponse(
        Long needsPlanId,
        Long companyId,
        int fiscalYear,
        Long costCenterId,
        Long financingSourceId,
        Long goalId) {
}
