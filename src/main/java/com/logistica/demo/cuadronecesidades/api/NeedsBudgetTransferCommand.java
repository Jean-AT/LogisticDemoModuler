package com.logistica.demo.cuadronecesidades.api;

import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.util.List;

public record NeedsBudgetTransferCommand(
        Long consolidationId,
        Long companyId,
        int fiscalYear,
        List<NeedsBudgetTransferLine> lines,
        IdempotencyKey idempotencyKey,
        String actor) {

    public NeedsBudgetTransferCommand {
        lines = List.copyOf(lines);
    }
}
