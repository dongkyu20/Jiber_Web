package com.jiber.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jiber.frontend")
public record FrontendProperties(
        @DefaultValue("http://localhost:5173") String publicBaseUrl
) {
}
