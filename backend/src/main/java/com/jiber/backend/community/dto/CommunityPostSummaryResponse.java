package com.jiber.backend.community.dto;

import java.time.OffsetDateTime;

public record CommunityPostSummaryResponse(
        Long postId,
        CommunityCategory category,
        String title,
        Long authorUserId,
        String authorDisplayName,
        long viewCount,
        long commentCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
