package com.logistica.demo.platform.dto;

public record NextDocumentNumberRequest(
        Long companyId,
        int fiscalYear,
        String documentType) {
}
