package com.logistica.demo.sharedkernel.idempotency;

import java.time.Instant;
import java.util.UUID;

public interface IdempotencyPort {

    IdempotencyClaim acquire(
            String operation,
            IdempotencyKey key,
            String requestHash,
            Instant requestedAt);

    void complete(UUID claimId, StoredResponse response, Instant completedAt);

    void release(UUID claimId);
}
