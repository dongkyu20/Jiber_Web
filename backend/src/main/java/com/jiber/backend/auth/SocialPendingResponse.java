package com.jiber.backend.auth;

public record SocialPendingResponse(
        String provider,
        String email,
        String displayName,
        boolean matchingEmailAccountExists
) {
}
