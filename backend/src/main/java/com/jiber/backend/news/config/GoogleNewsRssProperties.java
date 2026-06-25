package com.jiber.backend.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "jiber.news.google-rss")
public record GoogleNewsRssProperties(
        String baseUrl,
        String hl,
        String gl,
        String ceid
) {
    public String effectiveBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://news.google.com/rss/search";
        }
        return baseUrl.trim();
    }

    public String effectiveHl() {
        return StringUtils.hasText(hl) ? hl.trim() : "ko";
    }

    public String effectiveGl() {
        return StringUtils.hasText(gl) ? gl.trim() : "KR";
    }

    public String effectiveCeid() {
        return StringUtils.hasText(ceid) ? ceid.trim() : "KR:ko";
    }
}
