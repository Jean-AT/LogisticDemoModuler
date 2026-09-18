package com.logistica.demo.presupuesto.dto;

public record BudgetDimensionRequest(
        Long companyId,
        int fiscalYear,
        int month,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        Long expenseClassifierId) {
}
