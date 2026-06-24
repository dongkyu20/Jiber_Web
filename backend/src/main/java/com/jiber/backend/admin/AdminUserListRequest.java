package com.jiber.backend.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminUserListRequest(
        @PositiveOrZero Integer page,
        @Positive @Max(100) Integer size,
        String keyword,
        @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN만 지원합니다.") String role,
        Boolean enabled,
        String sort
) {
    public int effectivePage() {
        return page == null ? 0 : page;
    }

    public int effectiveSize() {
        return size == null ? 20 : size;
    }
}
