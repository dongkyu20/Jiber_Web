package com.jiber.backend.publicdata;

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
