package com.jiber.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void jiberOpenApiProvidesSwaggerMetadataAndBearerAuthScheme() {
        var openApi = new OpenApiConfig().jiberOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Jiber Backend API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getInfo().getDescription()).contains("부동산 거래 정보 플랫폼");

        var securitySchemes = openApi.getComponents().getSecuritySchemes();
        assertThat(securitySchemes).containsKey("bearerAuth");

        var bearerAuth = securitySchemes.get("bearerAuth");
        assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
    }
}
