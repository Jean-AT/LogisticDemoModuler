package com.logistica.demo.cuadronecesidades.api;

public interface NeedsBudgetTransferPort {

    NeedsBudgetTransferResult transfer(NeedsBudgetTransferCommand command);
}
