package com.logistica.demo.sharedkernel.idempotency;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria.");
        }
        value = value.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("La clave de idempotencia no puede superar 100 caracteres.");
        }
    }
}

