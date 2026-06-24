package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    @Test
    void removesKnownApiKeysFromMessages() {
        var redacted = SecretRedactor.redact("request failed with key real-public-data-key", "real-public-data-key");

        assertThat(redacted).doesNotContain("real-public-data-key");
        assertThat(redacted).contains("[REDACTED]");
    }
}
