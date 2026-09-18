package com.logistica.demo.presupuesto.dto;

public record BudgetPlanRequest(Long companyId, int fiscalYear, String notes) {
}
