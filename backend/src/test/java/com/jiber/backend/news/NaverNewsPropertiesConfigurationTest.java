package com.jiber.backend.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class NaverNewsPropertiesConfigurationTest {

    @Test
    void naverNewsConfigurationReadsNaverOAuthCredentialsForFallback() throws IOException {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test-oauth-naver",
                Map.of(
                        "OAUTH_NAVER_CLIENT_ID", "oauth-client-id",
                        "OAUTH_NAVER_CLIENT_SECRET", "oauth-client-secret"
                )
        ));

        for (var propertySource : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(propertySource);
        }

        assertThat(environment.getProperty("jiber.news.naver.oauth-client-id")).isEqualTo("oauth-client-id");
        assertThat(environment.getProperty("jiber.news.naver.oauth-client-secret")).isEqualTo("oauth-client-secret");
    }

    @Test
    void blankSearchCredentialsUseNaverOAuthCredentials() {
        var properties = new com.jiber.backend.news.config.NaverNewsProperties(
                "",
                "",
                "oauth-client-id",
                "oauth-client-secret",
                "https://openapi.naver.com"
        );

        assertThat(properties.effectiveClientId()).isEqualTo("oauth-client-id");
        assertThat(properties.effectiveClientSecret()).isEqualTo("oauth-client-secret");
    }
}
