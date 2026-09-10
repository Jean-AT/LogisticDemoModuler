package com.logistica.demo.platform.api;

import java.util.Objects;

public record AuditEventCommand(
        Long companyId,
        String actor,
        String effectiveRole,
        String action,
        String aggregateType,
        String aggregateId,
        String ipAddress,
        String traceId,
        String changesJson) {

    public AuditEventCommand {
        actor = requireText(actor, "actor");
        action = requireText(action, "action");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        if (companyId != null && companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
        effectiveRole = trimToNull(effectiveRole);
        ipAddress = trimToNull(ipAddress);
        traceId = trimToNull(traceId);
        changesJson = trimToNull(changesJson);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
