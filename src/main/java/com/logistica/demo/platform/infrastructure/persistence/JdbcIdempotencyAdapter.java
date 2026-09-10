package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaim;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcIdempotencyAdapter implements IdempotencyPort {

    private static final Duration EXPIRATION = Duration.ofHours(24);

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jsonSupport;

    public JdbcIdempotencyAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = new JdbcJsonSupport(jdbcTemplate);
    }

    @Override
    @Transactional
    public IdempotencyClaim acquire(
            String operation,
            IdempotencyKey key,
            String requestHash,
            Instant requestedAt) {
        String scope = normalize(operation, "operation");
        String hash = normalize(requestHash, "requestHash");
        Instant now = requestedAt == null ? Instant.now() : requestedAt;
        UUID claimId = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO platform.idempotency_keys (
                        id, scope, idempotency_key, request_hash, status, created_at, expires_at
                    ) VALUES (?, ?, ?, ?, 'PROCESSING', ?, ?)
                    """,
                    claimId,
                    scope,
                    key.value(),
                    hash,
                    now,
                    now.plus(EXPIRATION));
            return new IdempotencyClaim(IdempotencyClaimStatus.ACQUIRED, claimId, null);
        } catch (DuplicateKeyException ignored) {
            return readExisting(scope, key.value(), hash);
        }
    }

    @Override
    @Transactional
    public void complete(UUID claimId, StoredResponse response, Instant completedAt) {
        String sql = """
                UPDATE platform.idempotency_keys
                SET status = 'COMPLETED',
                    response_status = ?,
                    response_content_type = ?,
                    response_body = %s,
                    completed_at = ?
                WHERE id = ? AND status = 'PROCESSING'
                """.formatted(jsonSupport.jsonPlaceholder("JSONB"));
        int updated = jdbcTemplate.update(
                sql,
                response.statusCode(),
                response.contentType(),
                response.body(),
                completedAt == null ? Instant.now() : completedAt,
                claimId);
        if (updated == 0) {
            throw new IllegalStateException("La reserva de idempotencia no esta en procesamiento");
        }
    }

    @Override
    @Transactional
    public void release(UUID claimId) {
        jdbcTemplate.update(
                "DELETE FROM platform.idempotency_keys WHERE id = ? AND status = 'PROCESSING'",
                claimId);
    }

    private IdempotencyClaim readExisting(String scope, String key, String requestHash) {
        assertSameRequestHash(scope, key, requestHash);
        return jdbcTemplate.query(
                        """
                        SELECT id, request_hash, status, response_status, response_content_type, response_body
                        FROM platform.idempotency_keys
                        WHERE scope = ? AND idempotency_key = ?
                        """,
                        this::mapExisting,
                        scope,
                        key)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo leer la clave de idempotencia existente"));
    }

    private IdempotencyClaim mapExisting(ResultSet rs, int rowNumber) throws SQLException {
        String status = rs.getString("status");
        if ("COMPLETED".equals(status)) {
            return new IdempotencyClaim(
                    IdempotencyClaimStatus.COMPLETED,
                    uuid(rs),
                    new StoredResponse(
                            rs.getInt("response_status"),
                            rs.getString("response_content_type"),
                            rs.getString("response_body")));
        }
        return new IdempotencyClaim(IdempotencyClaimStatus.IN_PROGRESS, uuid(rs), null);
    }

    private void assertSameRequestHash(String scope, String key, String requestHash) {
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT request_hash FROM platform.idempotency_keys WHERE scope = ? AND idempotency_key = ?",
                String.class,
                scope,
                key);
        if (!requestHash.equals(storedHash)) {
            throw new IllegalArgumentException("La clave de idempotencia ya fue usada con otro contenido");
        }
    }

    private UUID uuid(ResultSet rs) throws SQLException {
        Object value = rs.getObject("id");
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
