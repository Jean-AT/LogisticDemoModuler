package com.logistica.demo.presupuesto.api;

public interface BudgetPlanUseCase {

    BudgetPlanResult generatePia(GenerateBudgetPlanCommand command);

    BudgetPlanResult reviewPia(ReviewBudgetPlanCommand command);

    BudgetPlanResult approvePiaAndCreateInitialPim(ApproveBudgetPlanCommand command);
}
