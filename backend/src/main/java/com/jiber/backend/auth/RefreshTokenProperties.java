package com.jiber.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jiber.auth.refresh-token")
public record RefreshTokenProperties(
        @DefaultValue("1209600") long ttlSeconds,
        @DefaultValue("local") String appEnv,
        @DefaultValue Cookie cookie
) {
    public record Cookie(
            @DefaultValue("JIBER_REFRESH_TOKEN") String name,
            @DefaultValue("/api/v1/auth") String path,
            @DefaultValue("Lax") String sameSite,
            @DefaultValue("false") boolean secure
    ) {
    }
}
