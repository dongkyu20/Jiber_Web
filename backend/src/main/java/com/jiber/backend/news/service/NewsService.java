package com.jiber.backend.news.service;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.news.client.GoogleNewsRssClient;
import com.jiber.backend.news.client.GoogleNewsRssClientException;
import com.jiber.backend.news.client.GoogleNewsRssItem;
import com.jiber.backend.news.dto.NewsFeedItemResponse;
import com.jiber.backend.news.dto.NewsFeedResponse;
import com.jiber.backend.news.dto.NewsSearchRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NewsService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final GoogleNewsRssClient googleNewsRssClient;

    public NewsService(GoogleNewsRssClient googleNewsRssClient) {
        this.googleNewsRssClient = googleNewsRssClient;
    }

    public NewsFeedResponse search(NewsSearchRequest request) {
        var keyword = request.effectiveQuery();
        var display = request.effectiveDisplay();

        try {
            var items = googleNewsRssClient.search(buildRealEstateQuery(keyword)).stream()
                    .map(this::toFeedItem)
                    .limit(display)
                    .toList();
            return new NewsFeedResponse(true, keyword, "Google 뉴스 RSS 검색 결과입니다.", items);
        } catch (GoogleNewsRssClientException exception) {
            throw new ApiException(ErrorCode.NEWS_SEARCH_UNAVAILABLE);
        }
    }

    private String buildRealEstateQuery(String keyword) {
        return keyword.contains("부동산") ? keyword : "%s 부동산".formatted(keyword);
    }

    private NewsFeedItemResponse toFeedItem(GoogleNewsRssItem item) {
        var source = cleanText(item.source());
        return new NewsFeedItemResponse(
                cleanText(item.title()),
                cleanText(item.description()),
                item.link(),
                item.link(),
                parsePublishedAt(item.pubDate()),
                source.isBlank() ? extractSource(item.link()) : source
        );
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        var unescaped = HtmlUtils.htmlUnescape(value);
        return HTML_TAG_PATTERN.matcher(unescaped).replaceAll("")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private OffsetDateTime parsePublishedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toOffsetDateTime();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String extractSource(String link) {
        if (link == null || link.isBlank()) {
            return "뉴스";
        }
        try {
            var host = URI.create(link).getHost();
            return host == null || host.isBlank() ? "뉴스" : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException exception) {
            return "뉴스";
        }
    }
}
