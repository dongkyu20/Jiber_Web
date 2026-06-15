package com.jiber.backend.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.common.config.CorsConfig;
import org.junit.jupiter.api.Test;

class CorsConfigTest {

    @Test
    void rejectsWildcardOriginWhenCredentialsAreEnabled() {
        var config = new CorsConfig();

        assertThatThrownBy(() -> config.corsConfigurationSource("http://localhost:5173,*"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }
}
