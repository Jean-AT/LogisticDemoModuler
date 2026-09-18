package com.logistica.demo.presupuesto.dto;

import java.util.List;

public record BudgetControlRequest(
        BudgetDocumentRequest source,
        List<BudgetAllocationRequest> allocations,
        String reason) {
}
