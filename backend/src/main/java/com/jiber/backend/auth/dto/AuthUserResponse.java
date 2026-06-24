package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.util.Set;

public record AuthUserResponse(
        Long userId,
        String email,
        String displayName,
        Set<String> roles
) {
    public static AuthUserResponse from(AuthUserPrincipal principal) {
        return new AuthUserResponse(
                principal.userId(),
                principal.email(),
                principal.displayName(),
                Set.copyOf(principal.roles())
        );
    }
}
