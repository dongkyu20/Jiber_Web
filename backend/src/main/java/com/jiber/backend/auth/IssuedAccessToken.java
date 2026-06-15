package com.jiber.backend.auth;

import java.time.Instant;

public record IssuedAccessToken(
        String token,
        String tokenType,
        long expiresIn,
        Instant expiresAt
) {
    public static IssuedAccessToken bearer(String token, long expiresIn, Instant expiresAt) {
        return new IssuedAccessToken(token, "Bearer", expiresIn, expiresAt);
    }
}
