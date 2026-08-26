package com.logistica.demo.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logistica.demo.shared.config.DemoJwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";

    private final DemoJwtProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(DemoJwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(String username, String role, String fullName) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.expirationMs());
        try {
            ObjectNode header = objectMapper.createObjectNode();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("sub", username);
            payload.put("role", role);
            payload.put("fullName", fullName);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", exp.getEpochSecond());

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signingInput);
            return signingInput + "." + signature;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo construir el token JWT.", ex);
        }
    }

    public TokenClaims parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Token JWT malformado.");
        }
        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        byte[] expected = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new InvalidTokenException("Firma del token JWT invalida.");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);
            long exp = payload.path("exp").asLong();
            if (exp > 0 && exp < Instant.now().getEpochSecond()) {
                throw new InvalidTokenException("El token JWT ha expirado.");
            }
            return new TokenClaims(
                    payload.path("sub").asText(),
                    payload.path("role").asText(),
                    payload.path("fullName").asText());
        } catch (IllegalArgumentException | java.io.IOException ex) {
            throw new InvalidTokenException("No se pudo decodificar el token JWT.");
        }
    }

    public String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el token JWT.", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenClaims(String username, String role, String fullName) {

        public boolean isValid() {
            return StringUtils.hasText(username) && StringUtils.hasText(role);
        }
    }
}
