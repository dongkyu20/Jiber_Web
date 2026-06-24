package com.jiber.backend.auth;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.controller.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void normalizesEmailByTrimmingAndLowerCasingWithRootLocale() {
        var normalizer = new EmailNormalizer();

        assertThat(normalizer.normalize(" USER@Example.COM ")).isEqualTo("user@example.com");
    }
}
