package com.logistica.demo.platform.api;

public record CatalogItemReference(
        Long id,
        Long companyId,
        String code,
        String name,
        String itemType,
        UnitOfMeasureReference unitOfMeasure,
        MasterDataReference expenseClassifier,
        boolean active) {

    public CatalogItemReference {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser positivo");
        }
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
        code = requireText(code, "code");
        name = requireText(name, "name");
        itemType = requireText(itemType, "itemType").toUpperCase(java.util.Locale.ROOT);
        java.util.Objects.requireNonNull(unitOfMeasure, "unitOfMeasure es obligatorio");
        java.util.Objects.requireNonNull(expenseClassifier, "expenseClassifier es obligatorio");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}
