package com.logistica.demo.platform.dto;

public record ConfigureDocumentSequenceRequest(
        Long companyId,
        int fiscalYear,
        String documentType,
        String prefix,
        long currentValue) {
}
