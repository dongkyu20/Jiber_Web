package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

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
