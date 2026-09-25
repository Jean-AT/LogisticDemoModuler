package com.logistica.demo.logistica.consultas.dto;

import java.time.LocalDateTime;

public record LogisticsTraceabilityEventResponse(
        String stage,
        String status,
        Long referenceId,
        String referenceNumber,
        String actor,
        LocalDateTime occurredAt,
        String detail) {
}
