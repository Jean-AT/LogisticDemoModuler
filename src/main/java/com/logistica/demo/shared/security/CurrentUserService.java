package com.logistica.demo.shared.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String getUsername() {
        return getAuthentication().getName();
    }

    public UserRole getRole() {
        return getRoles().stream()
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("No tiene un rol valido."));
    }

    public Set<UserRole> getRoles() {
        Set<UserRole> roles = getAuthentication().getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .map(UserRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        if (roles.isEmpty()) {
            throw new AccessDeniedException("No tiene un rol valido.");
        }
        return roles;
    }

    public boolean hasAnyRole(UserRole... roles) {
        Set<UserRole> currentRoles = getRoles();
        return Arrays.stream(roles).anyMatch(currentRoles::contains);
    }

    public boolean hasGlobalRequisitionAccess() {
        return hasAnyRole(UserRole.ADMIN, UserRole.APROBADOR, UserRole.COMPRAS);
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe autenticarse para acceder a este recurso.");
        }
        return authentication;
    }
}
