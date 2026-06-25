package com.jiber.backend.news.dto;

import java.util.List;

public record NewsFeedResponse(
        boolean available,
        String keyword,
        String message,
        List<NewsFeedItemResponse> items
) {
}
