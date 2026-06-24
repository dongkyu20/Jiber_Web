package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenCookieService {

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
        validateSecureCookiePolicy();
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(Duration.ofSeconds(properties.ttlSeconds()))
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
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
            throw new IllegalStateException("AUTH_REFRESH_TOKEN_COOKIE_SECURE must be true when APP_ENV is prod or production.");
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
