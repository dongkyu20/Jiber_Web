package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;

public record SocialAccountRecord(
        Long socialAccountId,
        Long userId,
        String oauthProvider,
        String providerUserId,
        String providerEmail,
        String providerDisplayName,
        OffsetDateTime linkedAt,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
