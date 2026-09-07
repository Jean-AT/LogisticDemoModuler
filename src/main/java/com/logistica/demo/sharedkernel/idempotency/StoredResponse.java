package com.logistica.demo.sharedkernel.idempotency;

import java.util.Objects;

public record StoredResponse(int statusCode, String contentType, String body) {

    public StoredResponse {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode HTTP invalido.");
        }
        Objects.requireNonNull(contentType, "contentType es obligatorio");
        Objects.requireNonNull(body, "body es obligatorio");
    }
}

