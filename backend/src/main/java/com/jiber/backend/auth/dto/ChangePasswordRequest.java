package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.service.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = PasswordPolicy.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
        String newPassword
) {
}
