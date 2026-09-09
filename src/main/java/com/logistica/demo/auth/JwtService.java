package com.logistica.demo.auth;

import com.logistica.demo.shared.config.DemoJwtProperties;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final DemoJwtProperties properties;
    private final JwtEncoder jwtEncoder;

    public JwtService(DemoJwtProperties properties, JwtEncoder jwtEncoder) {
        this.properties = properties;
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken(PlatformIdentity identity) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(properties.expirationMs());
        Set<String> roles = identity.accessProfile().roleCodes();
        Set<String> permissions = identity.accessProfile().permissions();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(identity.username())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("name", identity.fullName())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("token_type", "access")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expirationSeconds() {
        return properties.expirationMs() / 1000;
    }
}
