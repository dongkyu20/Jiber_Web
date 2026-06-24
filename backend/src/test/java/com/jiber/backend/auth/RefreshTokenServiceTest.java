package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T07:00:00Z"), ZoneOffset.UTC);
    private static final RefreshTokenProperties PROPERTIES = new RefreshTokenProperties(
            1209600,
            "local",
            new RefreshTokenProperties.Cookie("JIBER_REFRESH_TOKEN", "/api/v1/auth", "Lax", false)
    );

    @Test
    void rotatesRefreshTokenByRevokingCurrentSessionAndStoringOnlyNewTokenHash() {
        var mapper = new RecordingRefreshSessionMapper();
        var service = RefreshTokenService.forTesting(PROPERTIES, mapper, deterministicSecureRandom(), FIXED_CLOCK);
        var rawRefreshToken = "raw-refresh-token";
        var activeSession = session(10L, 1L, service.hash(rawRefreshToken), null);
        mapper.byTokenHash = activeSession;

        var result = service.rotate(rawRefreshToken, new RefreshRequestContext("JUnit", "127.0.0.1"));

        assertThat(mapper.revokedSessionId).isEqualTo(10L);
        assertThat(mapper.inserted).isNotNull();
        assertThat(mapper.inserted.rotatedFromSessionId()).isEqualTo(10L);
        assertThat(mapper.inserted.refreshTokenHash()).hasSize(64);
        assertThat(mapper.inserted.refreshTokenHash()).isNotEqualTo(result.token());
        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    void reusedRotatedRefreshTokenRevokesSessionFamilyAndReturnsAuthRequired() {
        var mapper = new RecordingRefreshSessionMapper();
        var service = RefreshTokenService.forTesting(PROPERTIES, mapper, deterministicSecureRandom(), FIXED_CLOCK);
        var rawRefreshToken = "already-rotated-refresh-token";
        var revokedSession = session(20L, 1L, service.hash(rawRefreshToken), OffsetDateTime.parse("2026-06-15T06:59:00Z"));
        mapper.byTokenHash = revokedSession;

        assertThatThrownBy(() -> service.rotate(rawRefreshToken, new RefreshRequestContext("JUnit", "127.0.0.1")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_REQUIRED);

        assertThat(mapper.revokedFamilyRootSessionId).isEqualTo(20L);
        assertThat(mapper.inserted).isNull();
    }

    private RefreshSessionRecord session(Long sessionId, Long userId, String tokenHash, OffsetDateTime revokedAt) {
        return new RefreshSessionRecord(
                sessionId,
                userId,
                tokenHash,
                null,
                "JUnit",
                null,
                OffsetDateTime.parse("2026-06-29T07:00:00Z"),
                revokedAt,
                OffsetDateTime.parse("2026-06-15T07:00:00Z"),
                OffsetDateTime.parse("2026-06-15T07:00:00Z")
        );
    }

    private SecureRandom deterministicSecureRandom() {
        return new SecureRandom(new byte[]{1, 2, 3, 4});
    }

    private static class RecordingRefreshSessionMapper implements RefreshSessionMapper {

        private RefreshSessionRecord byTokenHash;
        private RefreshSessionInsertCommand inserted;
        private Long revokedSessionId;
        private Long revokedFamilyRootSessionId;
        private Long revokedUserId;

        @Override
        public int insert(RefreshSessionInsertCommand command) {
            this.inserted = command;
            return 1;
        }

        @Override
        public RefreshSessionRecord findByTokenHash(String refreshTokenHash) {
            return byTokenHash;
        }

        @Override
        public RefreshSessionRecord findActiveByTokenHash(String refreshTokenHash, OffsetDateTime now) {
            return byTokenHash != null && byTokenHash.activeAt(now) ? byTokenHash : null;
        }

        @Override
        public int revokeBySessionId(Long refreshSessionId, OffsetDateTime revokedAt) {
            this.revokedSessionId = refreshSessionId;
            return 1;
        }

        @Override
        public int revokeByTokenHash(String refreshTokenHash, OffsetDateTime revokedAt) {
            return 1;
        }

        @Override
        public int revokeSessionFamily(Long refreshSessionId, OffsetDateTime revokedAt) {
            this.revokedFamilyRootSessionId = refreshSessionId;
            return 1;
        }

        @Override
        public int revokeByUserId(Long userId, OffsetDateTime revokedAt) {
            this.revokedUserId = userId;
            return 1;
        }
    }
}
