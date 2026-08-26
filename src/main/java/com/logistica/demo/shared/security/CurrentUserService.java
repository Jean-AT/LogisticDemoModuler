package com.logistica.demo.shared.security;

import java.util.Arrays;
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
        return getAuthentication().getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .map(UserRole::valueOf)
                .orElseThrow(() -> new AccessDeniedException("No tiene un rol valido."));
    }

    public boolean hasAnyRole(UserRole... roles) {
        UserRole currentRole = getRole();
        return Arrays.stream(roles).anyMatch(role -> role == currentRole);
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
