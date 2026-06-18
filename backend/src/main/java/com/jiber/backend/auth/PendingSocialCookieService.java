package com.jiber.backend.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PendingSocialCookieService {

    private final PendingSocialProperties properties;

    public PendingSocialCookieService(PendingSocialProperties properties) {
        this.properties = properties;
        validateSecureCookiePolicy();
    }

    public ResponseCookie createPendingCookie(String pendingToken) {
        return baseCookie(pendingToken)
                .maxAge(Duration.ofSeconds(properties.ttlSeconds()))
                .build();
    }

    public ResponseCookie clearPendingCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String cookieName() {
        return properties.cookie().name();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.cookie().name(), value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(properties.cookie().path());
    }

    private void validateSecureCookiePolicy() {
        if (isProductionLike(properties.appEnv()) && !properties.cookie().secure()) {
            throw new IllegalStateException("AUTH_PENDING_SOCIAL_COOKIE_SECURE must be true when APP_ENV is prod or production.");
        }
    }

    private boolean isProductionLike(String appEnv) {
        if (!StringUtils.hasText(appEnv)) {
            return false;
        }
        var normalized = appEnv.trim().toLowerCase();
        return normalized.equals("prod") || normalized.equals("production");
    }
}
