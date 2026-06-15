package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshTokenCookieServiceTest {

    @Test
    void createsHttpOnlyRefreshCookieWithConfiguredAttributes() {
        var properties = new RefreshTokenProperties(
                1209600,
                "local",
                new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
        );
        var service = new RefreshTokenCookieService(properties);

        var cookie = service.createRefreshCookie("raw-refresh-token");

        assertThat(cookie.getName()).isEqualTo("JIBER_REFRESH_TOKEN");
        assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(1209600));
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    void clearsRefreshCookieWithMatchingNameAndPath() {
        var properties = new RefreshTokenProperties(
                1209600,
                "local",
                new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", true)
        );
        var service = new RefreshTokenCookieService(properties);

        var cookie = service.clearRefreshCookie();

        assertThat(cookie.getName()).isEqualTo("JIBER_REFRESH_TOKEN");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    void rejectsInsecureRefreshCookieInProductionLikeEnvironment() {
        var properties = new RefreshTokenProperties(
                1209600,
                "production",
                new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new RefreshTokenCookieService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_REFRESH_TOKEN_COOKIE_SECURE");
    }
}
