package com.jiber.backend.auth;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
    public static AuthTokenResponse of(IssuedAccessToken token, AuthUserPrincipal principal) {
        return new AuthTokenResponse(token.token(), token.tokenType(), token.expiresIn(), AuthUserResponse.from(principal));
    }
}
