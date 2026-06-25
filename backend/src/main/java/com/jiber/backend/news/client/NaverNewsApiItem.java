package com.jiber.backend.news.client;

public record NaverNewsApiItem(
        String title,
        String link,
        String originallink,
        String description,
        String pubDate
) {
}
