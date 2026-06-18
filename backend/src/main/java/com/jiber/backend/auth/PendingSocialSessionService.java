package com.jiber.backend.auth;

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
public class PendingSocialSessionService {

    private final PendingSocialProperties properties;
    private final PendingSocialSessionMapper pendingSocialSessionMapper;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final EmailNormalizer emailNormalizer;

    @Autowired
    public PendingSocialSessionService(
            PendingSocialProperties properties,
            PendingSocialSessionMapper pendingSocialSessionMapper,
            EmailNormalizer emailNormalizer
    ) {
        this(properties, pendingSocialSessionMapper, new SecureRandom(), Clock.systemUTC(), emailNormalizer);
    }

    static PendingSocialSessionService forTesting(
            PendingSocialProperties properties,
            PendingSocialSessionMapper pendingSocialSessionMapper,
            SecureRandom secureRandom,
            Clock clock,
            EmailNormalizer emailNormalizer
    ) {
        return new PendingSocialSessionService(properties, pendingSocialSessionMapper, secureRandom, clock, emailNormalizer);
    }

    private PendingSocialSessionService(
            PendingSocialProperties properties,
            PendingSocialSessionMapper pendingSocialSessionMapper,
            SecureRandom secureRandom,
            Clock clock,
            EmailNormalizer emailNormalizer
    ) {
        this.properties = properties;
        this.pendingSocialSessionMapper = pendingSocialSessionMapper;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.emailNormalizer = emailNormalizer;
    }

    public IssuedPendingSocialToken issue(OAuth2ProviderUser providerUser) {
        if (providerUser == null || providerUser.provider() == null || !StringUtils.hasText(providerUser.providerUserId())) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
        }
        var token = newToken();
        var expiresAt = OffsetDateTime.now(clock).plusSeconds(properties.ttlSeconds());
        pendingSocialSessionMapper.insert(new PendingSocialSessionInsertCommand(
                hash(token),
                providerUser.provider().name(),
                providerUser.providerUserId(),
                providerUser.email(),
                providerUser.displayName(),
                StringUtils.hasText(providerUser.email()) ? emailNormalizer.normalize(providerUser.email()) : null,
                expiresAt
        ));
        return new IssuedPendingSocialToken(token, expiresAt);
    }

    public PendingSocialSessionRecord requireActive(String pendingToken) {
        if (!StringUtils.hasText(pendingToken)) {
            throw socialPendingNotFound();
        }
        var session = pendingSocialSessionMapper.findActiveByTokenHash(hash(pendingToken), OffsetDateTime.now(clock));
        if (session == null) {
            throw socialPendingNotFound();
        }
        return session;
    }

    public void consume(String pendingToken) {
        var updated = pendingSocialSessionMapper.consume(hash(pendingToken), OffsetDateTime.now(clock));
        if (updated == 0) {
            throw socialPendingNotFound();
        }
    }

    String hash(String pendingToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(pendingToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Pending social token hashing failed.", exception);
        }
    }

    private String newToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiException socialPendingNotFound() {
        return new ApiException(
                ErrorCode.SOCIAL_PENDING_NOT_FOUND,
                ErrorCode.SOCIAL_PENDING_NOT_FOUND.defaultMessage(),
                List.of()
        );
    }
}
