package com.logistica.demo.presupuesto.api;

public interface BudgetControlUseCase {

    BudgetControlResult precommit(PrecommitBudgetCommand command);

    BudgetControlResult commit(CommitBudgetCommand command);

    BudgetControlResult release(ReleaseBudgetCommand command);
}

