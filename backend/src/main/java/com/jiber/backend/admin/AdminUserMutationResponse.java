package com.jiber.backend.admin;

public record AdminUserMutationResponse(
        AdminUserSummaryResponse user,
        String message
) {
}
