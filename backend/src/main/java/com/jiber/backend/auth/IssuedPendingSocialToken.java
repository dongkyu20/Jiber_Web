package com.jiber.backend.auth;

import java.time.OffsetDateTime;

public record IssuedPendingSocialToken(
        String token,
        OffsetDateTime expiresAt
) {
}
