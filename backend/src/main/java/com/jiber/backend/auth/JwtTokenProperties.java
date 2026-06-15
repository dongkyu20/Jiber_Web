package com.jiber.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jiber.auth.jwt")
public record JwtTokenProperties(
        @DefaultValue("jiber-local") String issuer,
        @DefaultValue("") String secret,
        @DefaultValue("900") long accessTokenTtlSeconds,
        @DefaultValue("local") String appEnv
) {
}
