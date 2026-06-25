package com.jiber.backend.news.client;

public record GoogleNewsRssItem(
        String title,
        String link,
        String description,
        String pubDate,
        String source
) {
}
