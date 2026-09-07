package com.logistica.demo.presupuesto.api;

public interface TransferNeedsToBudgetUseCase {

    BudgetTransferResult transfer(TransferNeedsCommand command);
}

