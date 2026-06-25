package com.jiber.backend.news.service;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.news.client.NaverNewsApiItem;
import com.jiber.backend.news.client.NaverNewsClient;
import com.jiber.backend.news.client.NaverNewsClientException;
import com.jiber.backend.news.config.NaverNewsProperties;
import com.jiber.backend.news.dto.NewsFeedItemResponse;
import com.jiber.backend.news.dto.NewsFeedResponse;
import com.jiber.backend.news.dto.NewsSearchRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NewsService {

    private static final String LATEST_SORT = "date";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final NaverNewsClient naverNewsClient;
    private final NaverNewsProperties properties;

    public NewsService(NaverNewsClient naverNewsClient, NaverNewsProperties properties) {
        this.naverNewsClient = naverNewsClient;
        this.properties = properties;
    }

    public NewsFeedResponse search(NewsSearchRequest request) {
        var keyword = request.effectiveQuery();
        var display = request.effectiveDisplay();
        if (!properties.hasCredentials()) {
            return new NewsFeedResponse(
                    false,
                    keyword,
                    "네이버 뉴스 검색 API 키가 설정되지 않아 최신 뉴스를 불러올 수 없습니다.",
                    List.of()
            );
        }

        try {
            var response = naverNewsClient.search(keyword, display, LATEST_SORT);
            var items = response.safeItems().stream()
                    .map(this::toFeedItem)
                    .toList();
            return new NewsFeedResponse(true, keyword, "네이버 뉴스 검색 결과입니다.", items);
        } catch (NaverNewsClientException exception) {
            if (exception.isCredentialError()) {
                var message = exception.hasEmptyScopeError()
                        ? "네이버 개발자센터 앱 설정에서 검색 API 권한을 추가해 주세요. 네이버 로그인 권한만으로는 뉴스 검색 API를 사용할 수 없습니다."
                        : "네이버 뉴스 검색 API 키가 유효하지 않거나 검색 API 권한이 없습니다.";
                return new NewsFeedResponse(
                        false,
                        keyword,
                        message,
                        List.of()
                );
            }
            throw new ApiException(ErrorCode.NEWS_SEARCH_UNAVAILABLE);
        }
    }

    private NewsFeedItemResponse toFeedItem(NaverNewsApiItem item) {
        return new NewsFeedItemResponse(
                cleanText(item.title()),
                cleanText(item.description()),
                item.link(),
                item.originallink(),
                parsePublishedAt(item.pubDate()),
                extractSource(item.originallink(), item.link())
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

    private String extractSource(String originalLink, String fallbackLink) {
        var link = originalLink == null || originalLink.isBlank() ? fallbackLink : originalLink;
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
