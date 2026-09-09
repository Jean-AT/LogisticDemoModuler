package com.logistica.demo.platform.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ConfigureRolePermissionsCommand(
        String roleCode,
        Set<String> permissionCodes,
        String actor) {

    public ConfigureRolePermissionsCommand {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("roleCode es obligatorio");
        }
        roleCode = roleCode.trim().toUpperCase(Locale.ROOT);
        permissionCodes = Objects.requireNonNull(permissionCodes, "permissionCodes es obligatorio")
                .stream()
                .map(ConfigureRolePermissionsCommand::normalizePermission)
                .collect(Collectors.toUnmodifiableSet());
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        actor = actor.trim();
    }

    private static String normalizePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Los codigos de permiso no pueden estar vacios");
        }
        return permission.trim().toUpperCase(Locale.ROOT);
    }
}
