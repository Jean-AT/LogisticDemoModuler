package com.logistica.demo.auth.dto;

import com.logistica.demo.shared.security.UserRole;

public record UserResponse(Long id, String username, String fullName, UserRole role, boolean active) {
}
