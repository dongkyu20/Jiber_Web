package com.jiber.backend.community.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CommunityCommentResponse(
        Long commentId,
        Long postId,
        Long parentCommentId,
        Long authorUserId,
        String authorDisplayName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<CommunityCommentResponse> replies
) {
}
