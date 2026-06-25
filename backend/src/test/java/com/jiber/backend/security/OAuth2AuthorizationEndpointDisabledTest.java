package com.jiber.backend.security;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;
import com.jiber.backend.chat.controller.*;
import com.jiber.backend.property.dto.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiber.backend.auth.controller.AuthController;
import com.jiber.backend.auth.service.AuthService;
import com.jiber.backend.auth.mapper.AuthUserMapper;
import com.jiber.backend.auth.mapper.AuthUserRecord;
import com.jiber.backend.auth.service.EmailNormalizer;
import com.jiber.backend.auth.service.JwtAuthenticationFilter;
import com.jiber.backend.auth.config.JwtTokenProperties;
import com.jiber.backend.auth.service.JwtTokenService;
import com.jiber.backend.auth.service.PendingSocialCookieService;
import com.jiber.backend.auth.config.PendingSocialProperties;
import com.jiber.backend.auth.mapper.PendingSocialSessionInsertCommand;
import com.jiber.backend.auth.mapper.PendingSocialSessionMapper;
import com.jiber.backend.auth.mapper.PendingSocialSessionRecord;
import com.jiber.backend.auth.service.PendingSocialSessionService;
import com.jiber.backend.auth.mapper.RefreshSessionInsertCommand;
import com.jiber.backend.auth.mapper.RefreshSessionMapper;
import com.jiber.backend.auth.mapper.RefreshSessionRecord;
import com.jiber.backend.auth.service.RefreshTokenCookieService;
import com.jiber.backend.auth.config.RefreshTokenProperties;
import com.jiber.backend.auth.service.RefreshTokenService;
import com.jiber.backend.auth.mapper.SocialAccountInsertCommand;
import com.jiber.backend.auth.mapper.SocialAccountMapper;
import com.jiber.backend.auth.mapper.SocialAccountRecord;
import com.jiber.backend.auth.service.SocialLoginService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        JwtAuthenticationFilter.class,
        OAuth2AuthorizationEndpointDisabledTest.TestBeans.class
})
class OAuth2AuthorizationEndpointDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disabledOAuthAuthorizationEndpointReturnsSafeUnauthorized() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/oauth2/authorization/kakao"))
                .andExpect(content().string(not(containsString("INTERNAL_ERROR"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
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
        PendingSocialCookieService pendingSocialCookieService() {
            return new PendingSocialCookieService(
                    new PendingSocialProperties(
                            600,
                            "local",
                            new PendingSocialProperties.Cookie("JIBER_PENDING_SOCIAL", "/api/v1/auth", "Lax", false)
                    )
            );
        }

        @Bean
        AuthService authService(JwtTokenService jwtTokenService) {
            return new AuthService(jwtTokenService, null, null);
        }

        @Bean
        SocialLoginService socialLoginService(JwtTokenService jwtTokenService) {
            var refreshProperties = new RefreshTokenProperties(
                    1209600,
                    "local",
                    new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
            );
            var pendingProperties = new PendingSocialProperties(
                    600,
                    "local",
                    new PendingSocialProperties.Cookie("JIBER_PENDING_SOCIAL", "/api/v1/auth", "Lax", false)
            );
            return new SocialLoginService(
                    jwtTokenService,
                    new RefreshTokenService(refreshProperties, new NoopRefreshSessionMapper()),
                    new NoopAuthUserMapper(),
                    new NoopSocialAccountMapper(),
                    new PendingSocialSessionService(pendingProperties, new NoopPendingSocialSessionMapper(), new EmailNormalizer())
            );
        }

        private static class NoopAuthUserMapper implements AuthUserMapper {

            @Override
            public AuthUserRecord findById(Long userId) {
                return null;
            }

            @Override
            public AuthUserRecord findByEmail(String email) {
                return null;
            }

            @Override
            public int insertEmailUser(String email, String passwordHash, String displayName, String role, Boolean enabled, OffsetDateTime lastLoginAt) {
                return 0;
            }

            @Override
            public int updateLastLoginAt(Long userId, OffsetDateTime lastLoginAt) {
                return 0;
            }

            @Override
            public int updatePasswordHash(Long userId, String passwordHash, OffsetDateTime updatedAt) {
                return 0;
            }

            @Override
            public int updateDisplayName(Long userId, String displayName, OffsetDateTime updatedAt) {
                return 0;
            }

            @Override
            public int updateEnabled(Long userId, Boolean enabled, OffsetDateTime updatedAt) {
                return 0;
            }
        }

        private static class NoopSocialAccountMapper implements SocialAccountMapper {

            @Override
            public int insert(SocialAccountInsertCommand command) {
                return 0;
            }

            @Override
            public SocialAccountRecord findByProvider(String oauthProvider, String providerUserId) {
                return null;
            }

            @Override
            public AuthUserRecord findLinkedUserByProvider(String oauthProvider, String providerUserId) {
                return null;
            }

            @Override
            public List<SocialAccountRecord> findByUserId(Long userId) {
                return List.of();
            }

            @Override
            public int updateLastLoginAt(String oauthProvider, String providerUserId, OffsetDateTime lastLoginAt) {
                return 0;
            }
        }

        private static class NoopPendingSocialSessionMapper implements PendingSocialSessionMapper {

            @Override
            public int insert(PendingSocialSessionInsertCommand command) {
                return 0;
            }

            @Override
            public PendingSocialSessionRecord findByTokenHash(String pendingTokenHash) {
                return null;
            }

            @Override
            public PendingSocialSessionRecord findActiveByTokenHash(String pendingTokenHash, OffsetDateTime now) {
                return null;
            }

            @Override
            public int consume(String pendingTokenHash, OffsetDateTime consumedAt) {
                return 0;
            }
        }

        private static class NoopRefreshSessionMapper implements RefreshSessionMapper {

            @Override
            public int insert(RefreshSessionInsertCommand command) {
                return 0;
            }

            @Override
            public RefreshSessionRecord findByTokenHash(String refreshTokenHash) {
                return null;
            }

            @Override
            public RefreshSessionRecord findActiveByTokenHash(String refreshTokenHash, OffsetDateTime now) {
                return null;
            }

            @Override
            public int revokeBySessionId(Long refreshSessionId, OffsetDateTime revokedAt) {
                return 0;
            }

            @Override
            public int revokeByTokenHash(String refreshTokenHash, OffsetDateTime revokedAt) {
                return 0;
            }

            @Override
            public int revokeSessionFamily(Long refreshSessionId, OffsetDateTime revokedAt) {
                return 0;
            }

            @Override
            public int revokeByUserId(Long userId, OffsetDateTime revokedAt) {
                return 0;
            }
        }
    }
}
