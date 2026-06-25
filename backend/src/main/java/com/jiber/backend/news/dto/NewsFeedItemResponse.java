package com.jiber.backend.news.dto;

import java.time.OffsetDateTime;

public record NewsFeedItemResponse(
        String title,
        String summary,
        String naverLink,
        String originalLink,
        OffsetDateTime publishedAt,
        String source
) {
}
