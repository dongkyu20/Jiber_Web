package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T07:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuesAndValidatesAccessTokenWithConfiguredIssuerTtlAndRoles() {
        var properties = new JwtTokenProperties(
                "jiber-test",
                "test-secret-with-enough-entropy-for-hmac",
                900,
                "test"
        );
        var service = new JwtTokenService(properties, new ObjectMapper(), FIXED_CLOCK);
        var principal = new AuthUserPrincipal(1L, "user@example.com", "tester", Set.of("USER"));

        var issuedToken = service.issueAccessToken(principal);

        assertThat(issuedToken.token()).isNotBlank();
        assertThat(issuedToken.expiresIn()).isEqualTo(900);
        assertThat(issuedToken.expiresAt()).isEqualTo(Instant.parse("2026-06-15T07:15:00Z"));

        var parsedPrincipal = service.parseAccessToken(issuedToken.token()).orElseThrow();
        assertThat(parsedPrincipal.userId()).isEqualTo(1L);
        assertThat(parsedPrincipal.email()).isEqualTo("user@example.com");
        assertThat(parsedPrincipal.displayName()).isEqualTo("tester");
        assertThat(parsedPrincipal.roles()).containsExactly("USER");
    }

    @Test
    void rejectsBlankJwtSecretOutsideLocalLikeEnvironments() {
        var properties = new JwtTokenProperties("jiber-prod", "", 900, "prod");

        assertThatThrownBy(() -> new JwtTokenService(properties, new ObjectMapper(), FIXED_CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
