package com.jiber.backend.auth;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuth2LoginSuccessHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T07:00:00Z"), ZoneOffset.UTC);

    @Test
    void linkedOauthSuccessSetsRefreshCookieAndRedirectsToCallbackWithoutUrlToken() throws Exception {
        var fixture = new Fixture();
        fixture.authUserMapper.insertExistingUser(1L, "user@example.com", "$2a$10$testhashvaluefor mapper storage only", "사용자", true);
        fixture.socialAccountMapper.linkedUser = fixture.authUserMapper.findById(1L);
        var handler = fixture.handler();
        var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request(), response, googleAuthentication("google-1", "user@example.com", "사용자"));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login/callback");
        assertThat(response.getRedirectedUrl())
                .doesNotContain("access_token=")
                .doesNotContain("refresh_token=")
                .doesNotContain("provider_token=")
                .doesNotContain("token=");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("JIBER_REFRESH_TOKEN=")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand).isNull();
    }

    @Test
    void unlinkedOauthSuccessCreatesPendingSessionCookieAndRedirectsToSocialSignupWithoutCreatingUser() throws Exception {
        var fixture = new Fixture();
        var handler = fixture.handler();
        var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request(), response, googleAuthentication("google-2", "new@example.com", "새 사용자"));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/signup/social");
        assertThat(response.getRedirectedUrl())
                .doesNotContain("access_token=")
                .doesNotContain("refresh_token=")
                .doesNotContain("provider_token=")
                .doesNotContain("token=");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("JIBER_PENDING_SOCIAL=")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("JIBER_REFRESH_TOKEN=");

        var pendingToken = cookieValue(response.getHeader("Set-Cookie"), "JIBER_PENDING_SOCIAL");
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand).isNotNull();
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand.pendingTokenHash()).hasSize(64);
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand.pendingTokenHash()).isNotEqualTo(pendingToken);
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand.oauthProvider()).isEqualTo("GOOGLE");
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand.providerUserId()).isEqualTo("google-2");
        assertThat(fixture.pendingSocialSessionMapper.insertedCommand.providerEmail()).isEqualTo("new@example.com");
        assertThat(fixture.authUserMapper.insertCount).isZero();
        assertThat(fixture.refreshSessionMapper.insertCount).isZero();
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }

    private OAuth2AuthenticationToken googleAuthentication(String subject, String email, String name) {
        var oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("sub", subject, "email", email, "name", name),
                "sub"
        );
        return new OAuth2AuthenticationToken(oauthUser, oauthUser.getAuthorities(), "google");
    }

    private String cookieValue(String setCookie, String cookieName) {
        var prefix = cookieName + "=";
        var start = setCookie.indexOf(prefix);
        var end = setCookie.indexOf(';', start);
        return setCookie.substring(start + prefix.length(), end);
    }

    private static class Fixture {

        private final RefreshTokenProperties refreshProperties = new RefreshTokenProperties(
                1209600,
                "local",
                new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
        );
        private final PendingSocialProperties pendingProperties = new PendingSocialProperties(
                600,
                "local",
                new PendingSocialProperties.Cookie("JIBER_PENDING_SOCIAL", "/api/v1/auth", "Lax", false)
        );
        private final RecordingAuthUserMapper authUserMapper = new RecordingAuthUserMapper();
        private final RecordingSocialAccountMapper socialAccountMapper = new RecordingSocialAccountMapper();
        private final RecordingPendingSocialSessionMapper pendingSocialSessionMapper = new RecordingPendingSocialSessionMapper();
        private final RecordingRefreshSessionMapper refreshSessionMapper = new RecordingRefreshSessionMapper();

        private OAuth2LoginSuccessHandler handler() {
            var emailNormalizer = new EmailNormalizer();
            var pendingSocialSessionService = PendingSocialSessionService.forTesting(
                    pendingProperties,
                    pendingSocialSessionMapper,
                    new SecureRandom(new byte[]{1, 2, 3, 4}),
                    FIXED_CLOCK,
                    emailNormalizer
            );
            var socialLoginService = SocialLoginService.forTesting(
                    JwtTokenService.forTesting(
                            new JwtTokenProperties("jiber-test", "test-secret-with-enough-entropy-for-hmac", 900, "test"),
                            new ObjectMapper(),
                            FIXED_CLOCK
                    ),
                    RefreshTokenService.forTesting(
                            refreshProperties,
                            refreshSessionMapper,
                            new SecureRandom(new byte[]{5, 6, 7, 8}),
                            FIXED_CLOCK
                    ),
                    authUserMapper,
                    socialAccountMapper,
                    pendingSocialSessionService,
                    new BCryptPasswordEncoder(4),
                    emailNormalizer,
                    new PasswordPolicy(),
                    FIXED_CLOCK
            );
            return new OAuth2LoginSuccessHandler(
                    new OAuth2ProviderUserResolver(),
                    socialLoginService,
                    new RefreshTokenCookieService(refreshProperties),
                    new PendingSocialCookieService(pendingProperties),
                    new FrontendProperties("http://localhost:5173")
            );
        }
    }

    private static class RecordingAuthUserMapper implements AuthUserMapper {

        private final Map<Long, AuthUserRecord> usersById = new LinkedHashMap<>();
        private int insertCount;

        @Override
        public AuthUserRecord findById(Long userId) {
            return usersById.get(userId);
        }

        @Override
        public AuthUserRecord findByEmail(String email) {
            return usersById.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int insertEmailUser(String email, String passwordHash, String displayName, String role, Boolean enabled, OffsetDateTime lastLoginAt) {
            insertCount++;
            return 1;
        }

        @Override
        public int updateLastLoginAt(Long userId, OffsetDateTime lastLoginAt) {
            return 1;
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

        void insertExistingUser(Long userId, String email, String passwordHash, String displayName, Boolean enabled) {
            var now = OffsetDateTime.now(FIXED_CLOCK);
            usersById.put(userId, new AuthUserRecord(userId, email, passwordHash, displayName, "USER", enabled, now, now, now));
        }
    }

    private static class RecordingSocialAccountMapper implements SocialAccountMapper {

        private AuthUserRecord linkedUser;

        @Override
        public int insert(SocialAccountInsertCommand command) {
            throw new DuplicateKeyException("not used by handler test");
        }

        @Override
        public SocialAccountRecord findByProvider(String oauthProvider, String providerUserId) {
            return null;
        }

        @Override
        public AuthUserRecord findLinkedUserByProvider(String oauthProvider, String providerUserId) {
            return linkedUser;
        }

        @Override
        public List<SocialAccountRecord> findByUserId(Long userId) {
            return List.of();
        }

        @Override
        public int updateLastLoginAt(String oauthProvider, String providerUserId, OffsetDateTime lastLoginAt) {
            return linkedUser == null ? 0 : 1;
        }
    }

    private static class RecordingPendingSocialSessionMapper implements PendingSocialSessionMapper {

        private PendingSocialSessionInsertCommand insertedCommand;

        @Override
        public int insert(PendingSocialSessionInsertCommand command) {
            insertedCommand = command;
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

    private static class RecordingRefreshSessionMapper implements RefreshSessionMapper {

        private int insertCount;

        @Override
        public int insert(RefreshSessionInsertCommand command) {
            insertCount++;
            return 1;
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
