package com.jiber.backend.community;

import com.jiber.backend.community.dto.CommunityCategory;
import java.time.OffsetDateTime;

public record CommunityPostRow(
        Long postId,
        CommunityCategory category,
        String title,
        String content,
        Long authorUserId,
        String authorDisplayName,
        Long relatedPropertyId,
        String relatedPropertyName,
        String relatedPropertyAddress,
        Long viewCount,
        Long commentCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
