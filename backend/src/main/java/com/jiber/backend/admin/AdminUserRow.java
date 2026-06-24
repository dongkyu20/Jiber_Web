package com.jiber.backend.admin;

import java.time.OffsetDateTime;

public record AdminUserRow(
        Long userId,
        String email,
        String displayName,
        String role,
        Boolean enabled,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
