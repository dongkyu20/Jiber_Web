package com.jiber.backend.auth;

record AuthRefreshResult(
        AuthTokenResponse response,
        String refreshToken
) {
}
