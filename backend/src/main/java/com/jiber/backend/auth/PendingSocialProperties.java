package com.jiber.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jiber.auth.pending-social")
public record PendingSocialProperties(
        @DefaultValue("600") long ttlSeconds,
        @DefaultValue("local") String appEnv,
        @DefaultValue Cookie cookie
) {
    public record Cookie(
            @DefaultValue("JIBER_PENDING_SOCIAL") String name,
            @DefaultValue("/api/v1/auth") String path,
            @DefaultValue("Lax") String sameSite,
            @DefaultValue("false") boolean secure
    ) {
    }
}
