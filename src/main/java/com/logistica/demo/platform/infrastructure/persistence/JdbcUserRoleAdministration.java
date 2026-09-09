package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.AccessScope;
import com.logistica.demo.platform.api.AssignUserRoleCommand;
import com.logistica.demo.platform.api.ConfigureRolePermissionsCommand;
import com.logistica.demo.platform.api.UserRoleAdministration;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcUserRoleAdministration implements UserRoleAdministration {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRoleAdministration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Long assignRole(AssignUserRoleCommand command) {
        Long assignmentId = jdbcTemplate.query(
                """
                INSERT INTO platform.user_roles (
                    user_id, role_id, company_id, active, valid_from, valid_to, created_by, created_at
                )
                SELECT u.id, r.id, c.id, TRUE, CURRENT_TIMESTAMP, NULL, ?, CURRENT_TIMESTAMP
                FROM platform.users u
                JOIN platform.roles r ON r.code = ? AND r.active = TRUE
                JOIN platform.companies c ON c.id = ? AND c.active = TRUE
                WHERE u.id = ? AND u.active = TRUE
                ON CONFLICT (user_id, role_id, company_id)
                DO UPDATE SET active = TRUE, valid_to = NULL
                RETURNING id
                """,
                (resultSet, rowNumber) -> resultSet.getLong("id"),
                command.actor(),
                command.roleCode(),
                command.companyId(),
                command.userId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario, rol o compania inexistente o inactivo"));

        jdbcTemplate.update("DELETE FROM platform.user_scopes WHERE user_role_id = ?", assignmentId);
        for (AccessScope scope : command.scopes()) {
            validateScope(scope);
            jdbcTemplate.update(
                    """
                    INSERT INTO platform.user_scopes (
                        user_role_id, company_id, organization_unit_id, cost_center_id, created_by, created_at
                    ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    assignmentId,
                    scope.companyId(),
                    scope.organizationUnitId(),
                    scope.costCenterId(),
                    command.actor());
        }
        return assignmentId;
    }

    @Override
    @Transactional
    public void replacePermissions(ConfigureRolePermissionsCommand command) {
        Long roleId = jdbcTemplate.query(
                        "SELECT id FROM platform.roles WHERE code = ? AND active = TRUE",
                        (resultSet, rowNumber) -> resultSet.getLong("id"),
                        command.roleCode())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe o esta inactivo"));

        jdbcTemplate.update("DELETE FROM platform.role_permissions WHERE role_id = ?", roleId);
        for (String permissionCode : command.permissionCodes()) {
            int inserted = jdbcTemplate.update(
                    """
                    INSERT INTO platform.role_permissions (role_id, permission_id)
                    SELECT ?, id
                    FROM platform.permissions
                    WHERE code = ? AND active = TRUE
                    """,
                    roleId,
                    permissionCode);
            if (inserted == 0) {
                throw new IllegalArgumentException(
                        "El permiso no existe o esta inactivo: " + permissionCode);
            }
        }
    }

    @Override
    @Transactional
    public void revokeRole(Long userId, String roleCode, Long companyId, String actor) {
        if (userId == null
                || companyId == null
                || roleCode == null
                || roleCode.isBlank()
                || actor == null
                || actor.isBlank()) {
            throw new IllegalArgumentException("Usuario, rol, compania y actor son obligatorios");
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE platform.user_roles ur
                SET active = FALSE, valid_to = CURRENT_TIMESTAMP
                FROM platform.roles r
                WHERE ur.role_id = r.id
                  AND ur.user_id = ?
                  AND ur.company_id = ?
                  AND r.code = ?
                  AND ur.active = TRUE
                """,
                userId,
                companyId,
                roleCode.trim().toUpperCase(Locale.ROOT));
        if (updated == 0) {
            throw new IllegalArgumentException("La asignacion de rol activa no existe");
        }
    }

    private void validateScope(AccessScope scope) {
        if (scope.organizationUnitId() != null) {
            assertExists(
                    """
                    SELECT COUNT(*)
                    FROM platform.organization_units
                    WHERE id = ? AND company_id = ? AND active = TRUE
                    """,
                    "La unidad organizacional no pertenece a la compania o esta inactiva",
                    scope.organizationUnitId(),
                    scope.companyId());
        }
        if (scope.costCenterId() != null) {
            if (scope.organizationUnitId() == null) {
                assertExists(
                        """
                        SELECT COUNT(*)
                        FROM platform.cost_centers
                        WHERE id = ? AND company_id = ? AND active = TRUE
                        """,
                        "El centro de costo no pertenece a la compania o esta inactivo",
                        scope.costCenterId(),
                        scope.companyId());
            } else {
                assertExists(
                        """
                        SELECT COUNT(*)
                        FROM platform.cost_centers
                        WHERE id = ?
                          AND company_id = ?
                          AND organization_unit_id = ?
                          AND active = TRUE
                        """,
                        "El centro de costo no pertenece a la unidad indicada o esta inactivo",
                        scope.costCenterId(),
                        scope.companyId(),
                        scope.organizationUnitId());
            }
        }
    }

    private void assertExists(String sql, String message, Object... parameters) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, parameters);
        if (count == null || count == 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
