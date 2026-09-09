package com.logistica.demo.platform.api;

public interface UserRoleAdministration {

    Long assignRole(AssignUserRoleCommand command);

    void replacePermissions(ConfigureRolePermissionsCommand command);

    void revokeRole(Long userId, String roleCode, Long companyId, String actor);
}
