package com.jiber.backend.news.dto;

import java.time.OffsetDateTime;

public record NewsFeedItemResponse(
        String title,
        String summary,
        String link,
        String originalLink,
        OffsetDateTime publishedAt,
        String source
) {
}
