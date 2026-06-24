package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;

public record PendingSocialSessionRecord(
        Long pendingSocialSessionId,
        String pendingTokenHash,
        String oauthProvider,
        String providerUserId,
        String providerEmail,
        String providerDisplayName,
        String suggestedEmail,
        OffsetDateTime expiresAt,
        OffsetDateTime consumedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public boolean activeAt(OffsetDateTime now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }
}
