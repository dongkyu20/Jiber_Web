package com.jiber.backend.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AuthMeResponse(
        boolean authenticated,
        AuthUserResponse user
) {
    public static AuthMeResponse authenticated(AuthUserPrincipal principal) {
        return new AuthMeResponse(true, AuthUserResponse.from(principal));
    }

    public static AuthMeResponse unauthenticated() {
        return new AuthMeResponse(false, null);
    }
}
