package com.jiber.backend.admin;

import jakarta.validation.constraints.NotNull;

public record AdminUserRoleUpdateRequest(
        @NotNull AdminUserRole role
) {
}
