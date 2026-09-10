package com.logistica.demo.platform.api;

import java.util.Objects;

public record DocumentNumber(
        Long companyId,
        int fiscalYear,
        String documentType,
        String prefix,
        long value,
        String formatted) {

    public DocumentNumber {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        documentType = normalize(documentType, "documentType");
        prefix = normalize(prefix, "prefix");
        formatted = normalize(formatted, "formatted");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("value debe ser positivo");
        }
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}
