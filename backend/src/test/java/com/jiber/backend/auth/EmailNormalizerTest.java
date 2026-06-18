package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void normalizesEmailByTrimmingAndLowerCasingWithRootLocale() {
        var normalizer = new EmailNormalizer();

        assertThat(normalizer.normalize(" USER@Example.COM ")).isEqualTo("user@example.com");
    }
}
