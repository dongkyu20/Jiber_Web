package com.jiber.backend.security;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;
import com.jiber.backend.chat.controller.*;
import com.jiber.backend.property.dto.*;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiber.backend.auth.controller.AuthController;
import com.jiber.backend.auth.service.AuthService;
import com.jiber.backend.auth.mapper.AuthUserMapper;
import com.jiber.backend.auth.mapper.AuthUserRecord;
import com.jiber.backend.auth.config.FrontendProperties;
import com.jiber.backend.auth.service.JwtAuthenticationFilter;
import com.jiber.backend.auth.config.JwtTokenProperties;
import com.jiber.backend.auth.service.JwtTokenService;
import com.jiber.backend.auth.config.OAuth2ClientRegistrationProperties;
import com.jiber.backend.auth.service.OAuth2LoginSuccessHandler;
import com.jiber.backend.auth.service.OAuth2ProviderUserResolver;
import com.jiber.backend.auth.service.EmailNormalizer;
import com.jiber.backend.auth.service.PendingSocialCookieService;
import com.jiber.backend.auth.config.PendingSocialProperties;
import com.jiber.backend.auth.mapper.PendingSocialSessionInsertCommand;
import com.jiber.backend.auth.mapper.PendingSocialSessionMapper;
import com.jiber.backend.auth.mapper.PendingSocialSessionRecord;
import com.jiber.backend.auth.service.PendingSocialSessionService;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.extension.ExtendWith;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        JwtAuthenticationFilter.class,
        OAuth2AuthorizationEndpointTest.TestBeans.class
})
@ExtendWith(OutputCaptureExtension.class)
class OAuth2AuthorizationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredProviderAuthorizationEndpointRedirectsToProviderAuthorizationUri() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://kauth.kakao.com/oauth/authorize?")))
                .andExpect(header().string("Location", not(containsString("access_token="))))
                .andExpect(header().string("Location", not(containsString("refresh_token="))))
                .andExpect(header().string("Location", not(containsString("provider_token="))));
    }

    @Test
    void missingProviderAuthorizationEndpointReturnsSafeUnauthorizedWithoutExceptionLeak(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/oauth2/authorization/naver"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/oauth2/authorization/naver"))
                .andExpect(content().string(not(containsString("InvalidClientRegistrationIdException"))))
                .andExpect(content().string(not(containsString("stackTrace"))));

        org.assertj.core.api.Assertions.assertThat(output)
                .doesNotContain("InvalidClientRegistrationIdException");
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            var properties = new OAuth2ClientRegistrationProperties(
                    new OAuth2ClientRegistrationProperties.Provider("", "", ""),
                    new OAuth2ClientRegistrationProperties.Provider(
                            "dummy-kakao-client-id",
                            "dummy-kakao-client-secret",
                            "http://localhost:8080/login/oauth2/code/kakao"
                    ),
                    new OAuth2ClientRegistrationProperties.Provider("", "", "")
            );
            return new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(
                    properties.toClientRegistrations()
            );
        }

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
                    new RefreshTokenService(refreshProperties, new FakeRefreshSessionMapper()),
                    new FakeAuthUserMapper(),
                    new FakeSocialAccountMapper(),
                    new PendingSocialSessionService(pendingProperties, new FakePendingSocialSessionMapper(), new EmailNormalizer())
            );
        }

        @Bean
        OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
                RefreshTokenCookieService refreshTokenCookieService,
                PendingSocialCookieService pendingSocialCookieService,
                SocialLoginService socialLoginService
        ) {
            return new OAuth2LoginSuccessHandler(
                    new OAuth2ProviderUserResolver(),
                    socialLoginService,
                    refreshTokenCookieService,
                    pendingSocialCookieService,
                    new FrontendProperties("http://localhost:5173")
            );
        }

        private static class FakeRefreshSessionMapper implements com.jiber.backend.auth.mapper.RefreshSessionMapper {

            @Override
            public int insert(com.jiber.backend.auth.mapper.RefreshSessionInsertCommand command) {
                return 1;
            }

            @Override
            public com.jiber.backend.auth.mapper.RefreshSessionRecord findByTokenHash(String refreshTokenHash) {
                return null;
            }

            @Override
            public com.jiber.backend.auth.mapper.RefreshSessionRecord findActiveByTokenHash(String refreshTokenHash, OffsetDateTime now) {
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

        private static class FakePendingSocialSessionMapper implements PendingSocialSessionMapper {

            @Override
            public int insert(PendingSocialSessionInsertCommand command) {
                return 1;
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

        private static class FakeAuthUserMapper implements AuthUserMapper {

            @Override
            public AuthUserRecord findById(Long userId) {
                return null;
            }

            @Override
            public AuthUserRecord findByEmail(String email) {
                return null;
            }

            @Override
            public int insertEmailUser(
                    String email,
                    String passwordHash,
                    String displayName,
                    String role,
                    Boolean enabled,
                    OffsetDateTime lastLoginAt
            ) {
                return 0;
            }

            @Override
            public int updateLastLoginAt(Long userId, OffsetDateTime lastLoginAt) {
                return 1;
            }

            @Override
            public int updatePasswordHash(Long userId, String passwordHash, OffsetDateTime updatedAt) {
                return 0;
            }
        }

        private static class FakeSocialAccountMapper implements SocialAccountMapper {

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
                return new AuthUserRecord(
                        1L,
                        "oauth-user@example.test",
                        "$2a$10$testhashvaluefor mapper storage only",
                        "OAuth User",
                        "USER",
                        true,
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                );
            }

            @Override
            public List<SocialAccountRecord> findByUserId(Long userId) {
                return List.of();
            }

            @Override
            public int updateLastLoginAt(String oauthProvider, String providerUserId, OffsetDateTime lastLoginAt) {
                return 1;
            }
        }
    }
}
