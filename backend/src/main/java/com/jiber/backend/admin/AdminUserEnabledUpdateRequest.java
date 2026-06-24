package com.jiber.backend.admin;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledUpdateRequest(
        @NotNull Boolean enabled
) {
}
