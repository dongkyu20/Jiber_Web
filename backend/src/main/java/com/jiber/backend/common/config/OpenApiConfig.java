package com.jiber.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    OpenAPI jiberOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jiber Backend API")
                        .version("v1")
                        .description("지도 기반 부동산 거래 정보 플랫폼의 백엔드 API 문서입니다."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
