package com.logistica.demo.platform.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RoleGrant(String roleCode, Set<String> permissions, Set<AccessScope> scopes) {

    public RoleGrant {
        roleCode = normalize(roleCode, "roleCode");
        permissions = Objects.requireNonNull(permissions, "permissions es obligatorio")
                .stream()
                .map(permission -> normalize(permission, "permission"))
                .collect(Collectors.toUnmodifiableSet());
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes es obligatorio"));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("Un rol asignado requiere al menos un alcance");
        }
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.contains(normalize(permissionCode, "permissionCode"));
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
