package com.jiber.backend.community.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CommunityPostDetailResponse(
        Long postId,
        CommunityCategory category,
        String title,
        String content,
        Long authorUserId,
        String authorDisplayName,
        Long relatedPropertyId,
        String relatedPropertyName,
        String relatedPropertyAddress,
        long viewCount,
        long commentCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<CommunityCommentResponse> comments
) {
}
