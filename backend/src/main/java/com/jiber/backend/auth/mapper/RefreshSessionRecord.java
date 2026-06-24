package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;

public record RefreshSessionRecord(
        Long refreshSessionId,
        Long userId,
        String refreshTokenHash,
        Long rotatedFromSessionId,
        String userAgent,
        byte[] ipAddress,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public boolean activeAt(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
