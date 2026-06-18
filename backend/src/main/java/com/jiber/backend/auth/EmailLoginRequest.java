package com.jiber.backend.auth;

public record EmailLoginRequest(
        String email,
        String password
) {
}
