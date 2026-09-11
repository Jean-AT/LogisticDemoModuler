package com.logistica.demo.platform.api;

public record CurrencyReference(String code, String name, String symbol, int decimalPlaces, boolean active) {

    public CurrencyReference {
        code = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
        symbol = requireText(symbol, "symbol");
        if (decimalPlaces < 0 || decimalPlaces > 4) {
            throw new IllegalArgumentException("decimalPlaces debe estar entre 0 y 4");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}
