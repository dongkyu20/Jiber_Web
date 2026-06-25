package com.jiber.backend.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "jiber.news.naver")
public record NaverNewsProperties(
        String clientId,
        String clientSecret,
        String oauthClientId,
        String oauthClientSecret,
        String baseUrl
) {
    public boolean hasCredentials() {
        return StringUtils.hasText(effectiveClientId()) && StringUtils.hasText(effectiveClientSecret());
    }

    public String effectiveClientId() {
        return StringUtils.hasText(clientId) ? clientId.trim() : trimToEmpty(oauthClientId);
    }

    public String effectiveClientSecret() {
        return StringUtils.hasText(clientSecret) ? clientSecret.trim() : trimToEmpty(oauthClientSecret);
    }

    public String effectiveBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://openapi.naver.com";
        }
        var trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
