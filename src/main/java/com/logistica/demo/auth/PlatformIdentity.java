package com.logistica.demo.auth;

import com.logistica.demo.platform.api.UserAccessProfile;

public record PlatformIdentity(
        Long id,
        String username,
        String passwordHash,
        String fullName,
        boolean active,
        UserAccessProfile accessProfile) {
}
