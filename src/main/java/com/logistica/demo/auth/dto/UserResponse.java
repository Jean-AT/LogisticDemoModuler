package com.logistica.demo.auth.dto;

import com.logistica.demo.platform.api.AccessScope;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Set<String> roles,
        Set<String> permissions,
        Set<AccessScope> scopes,
        boolean active) {
}
