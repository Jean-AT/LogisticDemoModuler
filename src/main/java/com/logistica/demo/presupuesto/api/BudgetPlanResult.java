package com.logistica.demo.presupuesto.api;

public record BudgetPlanResult(
        Long piaExerciseId,
        Long pimExerciseId,
        int lines,
        boolean replayed) {
}
