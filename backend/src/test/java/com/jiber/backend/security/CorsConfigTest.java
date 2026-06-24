package com.jiber.backend.security;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;
import com.jiber.backend.chat.controller.*;
import com.jiber.backend.property.dto.*;

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
