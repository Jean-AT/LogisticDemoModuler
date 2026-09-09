package com.logistica.demo.platform.application;

import com.logistica.demo.platform.api.AccessPolicy;
import com.logistica.demo.platform.api.UserAccessQuery;
import org.springframework.stereotype.Service;

@Service
public class DefaultAccessPolicy implements AccessPolicy {

    private final UserAccessQuery userAccessQuery;

    public DefaultAccessPolicy(UserAccessQuery userAccessQuery) {
        this.userAccessQuery = userAccessQuery;
    }

    @Override
    public boolean isAllowed(
            String username,
            String permissionCode,
            Long companyId,
            Long organizationUnitId,
            Long costCenterId) {
        return userAccessQuery.findActiveByUsername(username)
                .map(profile -> profile.canAccess(
                        permissionCode,
                        companyId,
                        organizationUnitId,
                        costCenterId))
                .orElse(false);
    }
}
