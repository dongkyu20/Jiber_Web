package com.jiber.backend.auth;

public record AuthLogoutRequest(
        Boolean logoutAllDevices
) {
}
