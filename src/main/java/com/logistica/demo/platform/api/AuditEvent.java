package com.logistica.demo.platform.api;

import java.time.Instant;
import java.util.Objects;

public record AuditEvent(
        Long id,
        Long companyId,
        String actor,
        String effectiveRole,
        String action,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String ipAddress,
        String traceId,
        String changesJson) {

    public AuditEvent {
        Objects.requireNonNull(id, "id es obligatorio");
        Objects.requireNonNull(occurredAt, "occurredAt es obligatorio");
        actor = requireText(actor, "actor");
        action = requireText(action, "action");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        if (id <= 0) {
            throw new IllegalArgumentException("id debe ser positivo");
        }
        if (companyId != null && companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}
