package com.logistica.demo.sharedkernel.domain;

import java.util.Objects;

public record DocumentReference(String module, String type, Long id, String number) {

    public DocumentReference {
        module = requireText(module, "module");
        type = requireText(type, "type");
        Objects.requireNonNull(id, "id es obligatorio");
        number = requireText(number, "number");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }
}

