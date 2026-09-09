package com.logistica.demo.auth;

import com.logistica.demo.platform.api.UserAccessProfile;
import com.logistica.demo.platform.api.UserAccessQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformIdentityRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccessQuery userAccessQuery;

    public PlatformIdentityRepository(JdbcTemplate jdbcTemplate, UserAccessQuery userAccessQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccessQuery = userAccessQuery;
    }

    public Optional<PlatformIdentity> findActiveByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return findOne(
                """
                SELECT id, username, password_hash, full_name, active
                FROM platform.users
                WHERE LOWER(username) = LOWER(?) AND active = TRUE
                """,
                username.trim());
    }

    public Optional<PlatformIdentity> findActiveById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return findOne(
                """
                SELECT id, username, password_hash, full_name, active
                FROM platform.users
                WHERE id = ? AND active = TRUE
                """,
                userId);
    }

    private Optional<PlatformIdentity> findOne(String sql, Object parameter) {
        List<UserRow> users = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new UserRow(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("full_name"),
                        resultSet.getBoolean("active")),
                parameter);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        UserRow user = users.get(0);
        UserAccessProfile accessProfile = userAccessQuery.findActiveByUsername(user.username())
                .orElseGet(() -> new UserAccessProfile(user.id(), user.username(), List.of()));
        return Optional.of(new PlatformIdentity(
                user.id(),
                user.username(),
                user.passwordHash(),
                user.fullName(),
                user.active(),
                accessProfile));
    }

    private record UserRow(Long id, String username, String passwordHash, String fullName, boolean active) {
    }
}
