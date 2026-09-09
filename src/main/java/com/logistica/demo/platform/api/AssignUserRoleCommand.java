package com.logistica.demo.platform.api;

import java.util.Objects;
import java.util.Set;

public record AssignUserRoleCommand(
        Long userId,
        String roleCode,
        Long companyId,
        Set<AccessScope> scopes,
        String actor) {

    public AssignUserRoleCommand {
        Objects.requireNonNull(userId, "userId es obligatorio");
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        if (userId <= 0 || companyId <= 0) {
            throw new IllegalArgumentException("Los identificadores deben ser positivos");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("roleCode es obligatorio");
        }
        roleCode = roleCode.trim().toUpperCase(java.util.Locale.ROOT);
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes es obligatorio"));
        if (scopes.isEmpty() || scopes.stream().anyMatch(scope -> !companyId.equals(scope.companyId()))) {
            throw new IllegalArgumentException("Se requiere al menos un alcance de la misma compania");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        actor = actor.trim();
    }
}
