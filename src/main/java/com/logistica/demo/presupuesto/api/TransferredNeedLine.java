package com.logistica.demo.presupuesto.api;

import java.util.List;

public record TransferredNeedLine(
        Long needsPlanId,
        Long needsLineId,
        Long catalogItemId,
        List<TransferredNeedMonth> months) {

    public TransferredNeedLine {
        months = List.copyOf(months);
    }
}

