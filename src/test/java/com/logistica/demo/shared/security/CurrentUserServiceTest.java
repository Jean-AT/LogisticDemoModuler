package com.logistica.demo.shared.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecognizeAnyOfTheCurrentUserRoles() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "multirol",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_SOLICITANTE"),
                        new SimpleGrantedAuthority("ROLE_APROBADOR"),
                        new SimpleGrantedAuthority("CUADRO.APPROVE"))));

        assertTrue(currentUserService.hasAnyRole(UserRole.APROBADOR));
        assertTrue(currentUserService.hasAnyRole(UserRole.ADMIN, UserRole.SOLICITANTE));
        assertFalse(currentUserService.hasAnyRole(UserRole.COMPRAS));
    }
}
