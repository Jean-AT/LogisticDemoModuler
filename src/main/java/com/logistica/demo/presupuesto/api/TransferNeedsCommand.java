package com.logistica.demo.presupuesto.api;

import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.util.List;

public record TransferNeedsCommand(
        Long consolidationId,
        Long companyId,
        int fiscalYear,
        List<TransferredNeedLine> lines,
        IdempotencyKey idempotencyKey,
        String actor) {

    public TransferNeedsCommand {
        lines = List.copyOf(lines);
    }
}

