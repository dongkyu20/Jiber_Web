package com.jiber.backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiber.backend.auth.AuthController;
import com.jiber.backend.auth.AuthService;
import com.jiber.backend.auth.JwtAuthenticationFilter;
import com.jiber.backend.auth.JwtTokenProperties;
import com.jiber.backend.auth.JwtTokenService;
import com.jiber.backend.auth.RefreshTokenCookieService;
import com.jiber.backend.auth.RefreshTokenProperties;
import com.jiber.backend.favorite.FavoriteController;
import com.jiber.backend.favorite.FavoriteService;
import com.jiber.backend.notice.AdminNoticeController;
import com.jiber.backend.notice.NoticeService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        AuthController.class,
        FavoriteController.class,
        AdminNoticeController.class
})
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        JwtAuthenticationFilter.class,
        SecurityRulesTest.TestBeans.class
})
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMeAllowsAnonymousAndReturnsUnauthenticatedBody() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.user").value(Matchers.nullValue()));
    }

    @Test
    void favoritesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/favorites/apartments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void userCanAccessFavorites() throws Exception {
        mockMvc.perform(get("/api/v1/favorites/apartments")
                        .with(user("1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void adminNoticeMutationRequiresAdminRole() throws Exception {
        var body = """
                {
                  "title": "공지",
                  "content": "내용",
                  "pinned": false,
                  "publishedAt": "2026-06-15T16:00:00+09:00"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(user("1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminCanMutateNotices() throws Exception {
        var body = """
                {
                  "title": "공지",
                  "content": "내용",
                  "pinned": false,
                  "publishedAt": "2026-06-15T16:00:00+09:00"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(0));
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        JwtTokenService jwtTokenService(ObjectMapper objectMapper) {
            return new JwtTokenService(
                    new JwtTokenProperties("jiber-test", "test-secret-with-enough-entropy-for-hmac", 900, "test"),
                    objectMapper
            );
        }

        @Bean
        RefreshTokenCookieService refreshTokenCookieService() {
            return new RefreshTokenCookieService(
                    new RefreshTokenProperties(
                            1209600,
                            "local",
                            new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
                    )
            );
        }

        @Bean
        AuthService authService(JwtTokenService jwtTokenService) {
            return new AuthService(jwtTokenService, null, null);
        }

        @Bean
        FavoriteService favoriteService() {
            return new FavoriteService();
        }

        @Bean
        NoticeService noticeService() {
            return new NoticeService();
        }
    }
}
