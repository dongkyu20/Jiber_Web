package com.jiber.backend.community;

import java.time.OffsetDateTime;

public record CommunityCommentRow(
        Long commentId,
        Long postId,
        Long parentCommentId,
        Long authorUserId,
        String authorDisplayName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
