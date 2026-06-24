package com.jiber.backend.publicdata.service;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

public final class SecretRedactor {

    private SecretRedactor() {
    }

    public static String redact(String message, String secret) {
        if (message == null || secret == null || secret.isBlank()) {
            return message;
        }
        return message.replace(secret, "[REDACTED]");
    }
}
