package com.jiber.backend.auth;

import java.time.OffsetDateTime;

public record RefreshTokenRotationResult(
        String token,
        Long userId,
        OffsetDateTime expiresAt
) {
}
