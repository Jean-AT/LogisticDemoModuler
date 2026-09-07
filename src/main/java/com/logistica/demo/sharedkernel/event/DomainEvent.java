package com.logistica.demo.sharedkernel.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String aggregateType();

    String aggregateId();

    default String eventType() {
        return getClass().getSimpleName();
    }
}

