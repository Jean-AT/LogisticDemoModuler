package com.logistica.demo.platform.api;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record UserAccessProfile(Long userId, String username, List<RoleGrant> grants) {

    public UserAccessProfile {
        Objects.requireNonNull(userId, "userId es obligatorio");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username es obligatorio");
        }
        username = username.trim();
        grants = List.copyOf(Objects.requireNonNull(grants, "grants es obligatorio"));
    }

    public Set<String> roleCodes() {
        return grants.stream().map(RoleGrant::roleCode).collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> permissions() {
        return grants.stream()
                .flatMap(grant -> grant.permissions().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasRole(String roleCode) {
        String normalized = normalize(roleCode);
        return grants.stream().anyMatch(grant -> grant.roleCode().equals(normalized));
    }

    public boolean canAccess(
            String permissionCode,
            Long companyId,
            Long organizationUnitId,
            Long costCenterId) {
        return grants.stream()
                .filter(grant -> grant.hasPermission(permissionCode))
                .flatMap(grant -> grant.scopes().stream())
                .anyMatch(scope -> scope.covers(companyId, organizationUnitId, costCenterId));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("roleCode es obligatorio");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
