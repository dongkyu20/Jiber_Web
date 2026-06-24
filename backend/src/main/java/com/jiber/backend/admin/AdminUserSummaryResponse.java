package com.jiber.backend.admin;

import java.time.OffsetDateTime;

public record AdminUserSummaryResponse(
        Long userId,
        String email,
        String displayName,
        AdminUserRole role,
        boolean enabled,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
