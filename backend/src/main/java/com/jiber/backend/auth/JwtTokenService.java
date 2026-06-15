package com.jiber.backend.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtTokenProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] signingKey;

    public JwtTokenService(JwtTokenProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    JwtTokenService(JwtTokenProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.signingKey = resolveSigningKey(properties);
    }

    public IssuedAccessToken issueAccessToken(AuthUserPrincipal principal) {
        var now = Instant.now(clock);
        var expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());
        var header = Map.of("alg", "HS256", "typ", "JWT");
        var claims = Map.of(
                "iss", properties.issuer(),
                "sub", principal.userId().toString(),
                "email", nullToEmpty(principal.email()),
                "displayName", nullToEmpty(principal.displayName()),
                "roles", principal.roles().stream().sorted().toList(),
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );
        var signingInput = base64UrlJson(header) + "." + base64UrlJson(claims);
        var token = signingInput + "." + sign(signingInput);
        return IssuedAccessToken.bearer(token, properties.accessTokenTtlSeconds(), expiresAt);
    }

    public Optional<AuthUserPrincipal> parseAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        var parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        var signingInput = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        try {
            var claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!properties.issuer().equals(text(claims, "iss"))) {
                return Optional.empty();
            }
            if (Instant.now(clock).getEpochSecond() >= claims.path("exp").asLong()) {
                return Optional.empty();
            }
            var roles = roles(claims.path("roles"));
            if (roles.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new AuthUserPrincipal(
                    Long.parseLong(text(claims, "sub")),
                    emptyToNull(text(claims, "email")),
                    emptyToNull(text(claims, "displayName")),
                    roles
            ));
        } catch (RuntimeException | IOException exception) {
            return Optional.empty();
        }
    }

    private byte[] resolveSigningKey(JwtTokenProperties properties) {
        if (StringUtils.hasText(properties.secret())) {
            return properties.secret().getBytes(StandardCharsets.UTF_8);
        }
        if (isLocalLike(properties.appEnv())) {
            var generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            log.warn("JWT_SECRET is empty; using an ephemeral in-memory JWT signing key for local development. Tokens will be invalid after restart.");
            return generated;
        }
        throw new IllegalStateException("JWT_SECRET must be configured when APP_ENV is not local, dev, or test.");
    }

    private boolean isLocalLike(String appEnv) {
        if (!StringUtils.hasText(appEnv)) {
            return true;
        }
        var normalized = appEnv.trim().toLowerCase();
        return normalized.equals("local") || normalized.equals("dev") || normalized.equals("development") || normalized.equals("test");
    }

    private String base64UrlJson(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JWT claim serialization failed.", exception);
        }
    }

    private String sign(String signingInput) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signing failed.", exception);
        }
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private Set<String> roles(JsonNode rolesNode) {
        var roles = new LinkedHashSet<String>();
        if (rolesNode.isArray()) {
            rolesNode.forEach(role -> {
                if (StringUtils.hasText(role.asText())) {
                    roles.add(stripRolePrefix(role.asText()));
                }
            });
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String stripRolePrefix(String role) {
        return role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
