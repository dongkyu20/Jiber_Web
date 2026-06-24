package com.jiber.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountIdentifierRecoveryRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요.")
        String displayName
) {
}
