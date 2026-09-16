package com.logistica.demo.cuadronecesidades.api;

import java.util.List;

public record NeedsBudgetTransferLine(
        Long needsPlanId,
        Long needsLineId,
        Long catalogItemId,
        List<NeedsBudgetTransferMonth> months) {

    public NeedsBudgetTransferLine {
        months = List.copyOf(months);
    }
}
