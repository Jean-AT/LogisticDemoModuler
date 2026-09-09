package com.logistica.demo.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserAccessProfileTest {

    @Test
    void shouldExposeMultipleRolesAndPermissions() {
        UserAccessProfile profile = new UserAccessProfile(
                10L,
                "usuario",
                List.of(
                        grant("SOLICITANTE", "CUADRO.READ", new AccessScope(1L, null, null)),
                        grant("APROBADOR", "CUADRO.APPROVE", new AccessScope(1L, 20L, null))));

        assertEquals(Set.of("SOLICITANTE", "APROBADOR"), profile.roleCodes());
        assertEquals(Set.of("CUADRO.READ", "CUADRO.APPROVE"), profile.permissions());
        assertTrue(profile.hasRole("aprobador"));
    }

    @Test
    void shouldAllowOnlyTheGrantedCompanyScope() {
        UserAccessProfile profile = profile(grant(
                "ADMIN",
                "PLATFORM.MASTER.READ",
                new AccessScope(1L, null, null)));

        assertTrue(profile.canAccess("platform.master.read", 1L, 20L, 30L));
        assertFalse(profile.canAccess("PLATFORM.MASTER.READ", 2L, 20L, 30L));
    }

    @Test
    void shouldRestrictOrganizationAndCostCenterScopes() {
        UserAccessProfile profile = profile(grant(
                "SOLICITANTE",
                "CUADRO.WRITE",
                new AccessScope(1L, 20L, 30L)));

        assertTrue(profile.canAccess("CUADRO.WRITE", 1L, 20L, 30L));
        assertFalse(profile.canAccess("CUADRO.WRITE", 1L, 21L, 30L));
        assertFalse(profile.canAccess("CUADRO.WRITE", 1L, 20L, 31L));
        assertFalse(profile.canAccess("CUADRO.READ", 1L, 20L, 30L));
    }

    @Test
    void shouldRejectAssignmentWithScopeFromAnotherCompany() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AssignUserRoleCommand(
                        10L,
                        "ADMIN",
                        1L,
                        Set.of(new AccessScope(2L, null, null)),
                        "system"));
    }

    @Test
    void shouldNormalizeConfiguredPermissionCodes() {
        ConfigureRolePermissionsCommand command = new ConfigureRolePermissionsCommand(
                " administrador ",
                Set.of(" plataforma.usuario.leer ", "PLATAFORMA.USUARIO.EDITAR"),
                " admin ");

        assertEquals("ADMINISTRADOR", command.roleCode());
        assertEquals(
                Set.of("PLATAFORMA.USUARIO.LEER", "PLATAFORMA.USUARIO.EDITAR"),
                command.permissionCodes());
        assertEquals("admin", command.actor());
    }

    private UserAccessProfile profile(RoleGrant grant) {
        return new UserAccessProfile(10L, "usuario", List.of(grant));
    }

    private RoleGrant grant(String role, String permission, AccessScope scope) {
        return new RoleGrant(role, Set.of(permission), Set.of(scope));
    }
}
