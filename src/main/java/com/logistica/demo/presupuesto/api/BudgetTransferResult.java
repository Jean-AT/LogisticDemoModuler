package com.logistica.demo.presupuesto.api;

public record BudgetTransferResult(
        Long transferId,
        Long unitBudgetExerciseId,
        int transferredLines,
        boolean replayed) {
}

