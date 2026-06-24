package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = PasswordPolicy.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "표시 이름을 입력해 주세요.")
        String displayName
) {
}
