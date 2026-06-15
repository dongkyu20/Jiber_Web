package com.jiber.backend.auth;

public record OAuth2ProviderUser(
        OAuth2Provider provider,
        String providerUserId,
        String email,
        String displayName
) {
}
