package com.jiber.backend.notice;

import java.time.OffsetDateTime;

public record NoticeRow(
        Long noticeId,
        String title,
        String content,
        Boolean pinned,
        OffsetDateTime publishedAt,
        Long createdByUserId,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
