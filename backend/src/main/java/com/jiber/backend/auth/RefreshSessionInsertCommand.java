package com.jiber.backend.auth;

import java.time.OffsetDateTime;

public record RefreshSessionInsertCommand(
        Long userId,
        String refreshTokenHash,
        Long rotatedFromSessionId,
        String userAgent,
        byte[] ipAddress,
        OffsetDateTime expiresAt
) {
}
