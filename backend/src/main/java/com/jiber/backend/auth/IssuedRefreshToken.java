package com.jiber.backend.auth;

import java.time.OffsetDateTime;

public record IssuedRefreshToken(
        String token,
        Long userId,
        OffsetDateTime expiresAt
) {
}
