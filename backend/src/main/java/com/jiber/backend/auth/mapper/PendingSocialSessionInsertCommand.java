package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;

public record PendingSocialSessionInsertCommand(
        String pendingTokenHash,
        String oauthProvider,
        String providerUserId,
        String providerEmail,
        String providerDisplayName,
        String suggestedEmail,
        OffsetDateTime expiresAt
) {
}
