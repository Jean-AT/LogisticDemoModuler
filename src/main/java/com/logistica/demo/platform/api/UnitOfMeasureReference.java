package com.logistica.demo.platform.api;

public record UnitOfMeasureReference(Long id, String code, String name, boolean active) {

    public UnitOfMeasureReference {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser positivo");
        }
        code = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}
