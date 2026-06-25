package com.jiber.backend.news;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.news.config.GoogleNewsRssProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class GoogleNewsRssPropertiesConfigurationTest {

    @Test
    void googleNewsRssConfigurationUsesKoreanDefaults() throws IOException {
        var environment = new StandardEnvironment();
        var loader = new YamlPropertySourceLoader();
        loader.load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);

        assertThat(environment.getProperty("jiber.news.google-rss.base-url"))
                .isEqualTo("https://news.google.com/rss/search");
        assertThat(environment.getProperty("jiber.news.google-rss.hl")).isEqualTo("ko");
        assertThat(environment.getProperty("jiber.news.google-rss.gl")).isEqualTo("KR");
        assertThat(environment.getProperty("jiber.news.google-rss.ceid")).isEqualTo("KR:ko");
    }

    @Test
    void blankValuesFallbackToKoreanDefaults() {
        var properties = new GoogleNewsRssProperties("", "", "", "");

        assertThat(properties.effectiveBaseUrl()).isEqualTo("https://news.google.com/rss/search");
        assertThat(properties.effectiveHl()).isEqualTo("ko");
        assertThat(properties.effectiveGl()).isEqualTo("KR");
        assertThat(properties.effectiveCeid()).isEqualTo("KR:ko");
    }
}
