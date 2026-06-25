package com.jiber.backend.security;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;
import com.jiber.backend.chat.controller.*;
import com.jiber.backend.property.dto.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.common.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    @Test
    void rejectsWildcardOriginWhenCredentialsAreEnabled() {
        var config = new CorsConfig();

        assertThatThrownBy(() -> config.corsConfigurationSource("http://localhost:5173,*"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    void allowsPatchPreflightForAccountManagementApis() {
        var config = new CorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource("http://localhost:5173");
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/account/password");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "PATCH");

        var corsConfiguration = source.getCorsConfiguration(request);

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedMethods()).contains("PATCH");
    }
}
