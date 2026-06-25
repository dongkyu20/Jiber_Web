package com.jiber.backend.news.client;

import java.util.List;

public record NaverNewsApiResponse(
        String lastBuildDate,
        Integer total,
        Integer start,
        Integer display,
        List<NaverNewsApiItem> items
) {
    public List<NaverNewsApiItem> safeItems() {
        return items == null ? List.of() : items;
    }
}
