package com.jiber.backend.common.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${jiber.cors.allowed-origins:http://localhost:5173}") String allowedOrigins
    ) {
        var parsedOrigins = parseCsv(allowedOrigins);
        validateCredentialedOrigins(parsedOrigins);

        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parsedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private List<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private void validateCredentialedOrigins(List<String> origins) {
        if (origins.contains("*")) {
            throw new IllegalStateException("Credentialed CORS must not allow wildcard origins. Configure BACKEND_CORS_ALLOWED_ORIGINS with explicit origins.");
        }
    }
}
