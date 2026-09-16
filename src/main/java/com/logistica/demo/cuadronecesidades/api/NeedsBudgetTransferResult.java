package com.logistica.demo.cuadronecesidades.api;

public record NeedsBudgetTransferResult(
        Long transferId,
        Long unitBudgetExerciseId,
        int transferredLines,
        boolean replayed) {
}
