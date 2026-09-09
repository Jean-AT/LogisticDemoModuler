package com.logistica.demo.platform.api;

import java.util.Optional;

public interface UserAccessQuery {

    Optional<UserAccessProfile> findActiveByUsername(String username);
}
