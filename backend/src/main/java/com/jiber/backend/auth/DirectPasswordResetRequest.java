package com.jiber.backend.auth;

import com.jiber.backend.auth.service.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectPasswordResetRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 320, message = "이메일은 320자 이하로 입력해 주세요.")
        String email,

        @NotBlank(message = "표시 이름을 입력해 주세요.")
        @Size(max = 100, message = "표시 이름은 100자 이하로 입력해 주세요.")
        String displayName,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = PasswordPolicy.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
        String newPassword
) {
}
