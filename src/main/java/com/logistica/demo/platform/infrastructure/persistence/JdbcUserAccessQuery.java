package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.AccessScope;
import com.logistica.demo.platform.api.RoleGrant;
import com.logistica.demo.platform.api.UserAccessProfile;
import com.logistica.demo.platform.api.UserAccessQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccessQuery implements UserAccessQuery {

    private static final String ACTIVE_ACCESS_SQL = """
            SELECT u.id AS user_id,
                   u.username,
                   ur.id AS user_role_id,
                   r.code AS role_code,
                   COALESCE(us.company_id, ur.company_id) AS scope_company_id,
                   us.organization_unit_id,
                   us.cost_center_id,
                   p.code AS permission_code
            FROM platform.users u
            JOIN platform.user_roles ur
              ON ur.user_id = u.id
             AND ur.active = TRUE
             AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_TIMESTAMP)
             AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_TIMESTAMP)
            JOIN platform.roles r ON r.id = ur.role_id AND r.active = TRUE
            LEFT JOIN platform.user_scopes us ON us.user_role_id = ur.id
            LEFT JOIN platform.role_permissions rp ON rp.role_id = r.id
            LEFT JOIN platform.permissions p ON p.id = rp.permission_id AND p.active = TRUE
            WHERE LOWER(u.username) = LOWER(?)
              AND u.active = TRUE
            ORDER BY ur.id, us.id, p.code
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccessQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccessProfile> findActiveByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(ACTIVE_ACCESS_SQL, this::extractProfile, username.trim());
    }

    private Optional<UserAccessProfile> extractProfile(ResultSet resultSet) throws SQLException {
        Long userId = null;
        String username = null;
        Map<Long, GrantBuilder> grants = new LinkedHashMap<>();

        while (resultSet.next()) {
            userId = resultSet.getLong("user_id");
            username = resultSet.getString("username");
            long assignmentId = resultSet.getLong("user_role_id");
            GrantBuilder grant = grants.computeIfAbsent(
                    assignmentId,
                    ignored -> new GrantBuilder(resultSetValue(resultSet, "role_code")));

            grant.scopes.add(new AccessScope(
                    resultSet.getLong("scope_company_id"),
                    nullableLong(resultSet, "organization_unit_id"),
                    nullableLong(resultSet, "cost_center_id")));

            String permission = resultSet.getString("permission_code");
            if (permission != null) {
                grant.permissions.add(permission);
            }
        }

        if (userId == null) {
            return Optional.empty();
        }
        var roleGrants = grants.values().stream().map(GrantBuilder::build).toList();
        return Optional.of(new UserAccessProfile(userId, username, roleGrants));
    }

    private String resultSetValue(ResultSet resultSet, String column) {
        try {
            return resultSet.getString(column);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el perfil de acceso", ex);
        }
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static final class GrantBuilder {

        private final String roleCode;
        private final Set<String> permissions = new LinkedHashSet<>();
        private final Set<AccessScope> scopes = new LinkedHashSet<>();

        private GrantBuilder(String roleCode) {
            this.roleCode = roleCode;
        }

        private RoleGrant build() {
            return new RoleGrant(roleCode, permissions, scopes);
        }
    }
}
