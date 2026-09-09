package com.logistica.demo.auth;

import com.logistica.demo.shared.config.DemoJwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PlatformIdentityRepository identityRepository;
    private final DemoJwtProperties properties;

    public RefreshTokenService(
            JdbcTemplate jdbcTemplate,
            PlatformIdentityRepository identityRepository,
            DemoJwtProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.identityRepository = identityRepository;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issue(Long userId) {
        return persist(userId);
    }

    @Transactional
    public RefreshGrant rotate(String rawToken) {
        String tokenHash = hash(requireToken(rawToken));
        List<StoredToken> matches = jdbcTemplate.query(
                """
                SELECT id, user_id
                FROM platform.refresh_tokens
                WHERE token_hash = ?
                  AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new StoredToken(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getLong("user_id")),
                tokenHash);
        if (matches.isEmpty()) {
            throw new BadCredentialsException("Refresh token invalido, expirado o revocado.");
        }

        StoredToken current = matches.get(0);
        PlatformIdentity identity = identityRepository.findActiveById(current.userId())
                .orElseThrow(() -> new BadCredentialsException("El usuario esta inactivo."));
        IssuedRefreshToken replacement = persist(current.userId());
        int updated = jdbcTemplate.update(
                """
                UPDATE platform.refresh_tokens
                SET revoked_at = CURRENT_TIMESTAMP, replaced_by = ?
                WHERE id = ? AND revoked_at IS NULL
                """,
                replacement.id(),
                current.id());
        if (updated != 1) {
            throw new BadCredentialsException("El refresh token ya fue utilizado.");
        }
        return new RefreshGrant(identity, replacement.rawToken());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE platform.refresh_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                """,
                hash(rawToken.trim()));
    }

    private IssuedRefreshToken persist(Long userId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(properties.refreshExpirationMs());
        jdbcTemplate.update(
                """
                INSERT INTO platform.refresh_tokens (
                    id, user_id, token_hash, issued_at, expires_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                id,
                userId,
                hash(rawToken),
                issuedAt,
                expiresAt);
        return new IssuedRefreshToken(id, rawToken);
    }

    private String requireToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token obligatorio.");
        }
        return rawToken.trim();
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    public record IssuedRefreshToken(UUID id, String rawToken) {
    }

    public record RefreshGrant(PlatformIdentity identity, String refreshToken) {
    }

    private record StoredToken(UUID id, Long userId) {
    }
}
