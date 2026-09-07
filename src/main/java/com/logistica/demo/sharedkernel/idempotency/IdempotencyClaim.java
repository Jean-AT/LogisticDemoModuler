package com.logistica.demo.sharedkernel.idempotency;

import java.util.UUID;

public record IdempotencyClaim(
        IdempotencyClaimStatus status,
        UUID claimId,
        StoredResponse storedResponse) {

    public IdempotencyClaim {
        if (status == null) {
            throw new IllegalArgumentException("status es obligatorio");
        }
        if (status == IdempotencyClaimStatus.ACQUIRED && claimId == null) {
            throw new IllegalArgumentException("claimId es obligatorio para una reserva adquirida");
        }
        if (status == IdempotencyClaimStatus.COMPLETED && storedResponse == null) {
            throw new IllegalArgumentException("storedResponse es obligatorio para una operacion completada");
        }
    }
}

