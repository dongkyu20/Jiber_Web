package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.Instant;

public record IssuedAccessToken(
        String token,
        String tokenType,
        long expiresIn,
        Instant expiresAt
) {
    public static IssuedAccessToken bearer(String token, long expiresIn, Instant expiresAt) {
        return new IssuedAccessToken(token, "Bearer", expiresIn, expiresAt);
    }
}
