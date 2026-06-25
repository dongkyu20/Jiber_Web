package com.jiber.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
