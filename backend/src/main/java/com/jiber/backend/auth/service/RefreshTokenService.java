package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

    private final RefreshTokenProperties properties;
    private final RefreshSessionMapper refreshSessionMapper;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(RefreshTokenProperties properties, RefreshSessionMapper refreshSessionMapper) {
        this(properties, refreshSessionMapper, new SecureRandom(), Clock.systemUTC());
    }

    public static RefreshTokenService forTesting(
            RefreshTokenProperties properties,
            RefreshSessionMapper refreshSessionMapper,
            SecureRandom secureRandom,
            Clock clock
    ) {
        return new RefreshTokenService(properties, refreshSessionMapper, secureRandom, clock);
    }

    private RefreshTokenService(
            RefreshTokenProperties properties,
            RefreshSessionMapper refreshSessionMapper,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.properties = properties;
        this.refreshSessionMapper = refreshSessionMapper;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    public IssuedRefreshToken issue(Long userId, RefreshRequestContext context) {
        return issue(userId, null, context);
    }

    public RefreshTokenRotationResult rotate(String rawRefreshToken, RefreshRequestContext context) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw authRequired();
        }
        var now = OffsetDateTime.now(clock);
        var tokenHash = hash(rawRefreshToken);
        var session = refreshSessionMapper.findByTokenHash(tokenHash);
        if (session == null) {
            throw authRequired();
        }
        if (session.revokedAt() != null) {
            refreshSessionMapper.revokeSessionFamily(session.refreshSessionId(), now);
            throw authRequired();
        }
        if (!session.expiresAt().isAfter(now)) {
            throw authRequired();
        }
        refreshSessionMapper.revokeBySessionId(session.refreshSessionId(), now);
        var issued = issue(session.userId(), session.refreshSessionId(), context);
        return new RefreshTokenRotationResult(issued.token(), issued.userId(), issued.expiresAt());
    }

    public void revoke(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        refreshSessionMapper.revokeByTokenHash(hash(rawRefreshToken), OffsetDateTime.now(clock));
    }

    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        refreshSessionMapper.revokeByUserId(userId, OffsetDateTime.now(clock));
    }

    public String hash(String rawRefreshToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh token hashing failed.", exception);
        }
    }

    private IssuedRefreshToken issue(Long userId, Long rotatedFromSessionId, RefreshRequestContext context) {
        var rawToken = newRawToken();
        var expiresAt = OffsetDateTime.now(clock).plusSeconds(properties.ttlSeconds());
        var command = new RefreshSessionInsertCommand(
                userId,
                hash(rawToken),
                rotatedFromSessionId,
                context == null ? null : context.userAgent(),
                context == null ? null : context.remoteAddressBytes(),
                expiresAt
        );
        refreshSessionMapper.insert(command);
        return new IssuedRefreshToken(rawToken, userId, expiresAt);
    }

    private String newRawToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiException authRequired() {
        return new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
    }
}
