package com.logistica.demo.platform.api;

public interface AccessPolicy {

    boolean isAllowed(
            String username,
            String permissionCode,
            Long companyId,
            Long organizationUnitId,
            Long costCenterId);
}
