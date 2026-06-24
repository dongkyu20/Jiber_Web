package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.common.error.ErrorDetail;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    public void validate(String password) {
        if (!StringUtils.hasText(password) || password.length() < MIN_LENGTH) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.defaultMessage(),
                    List.of(new ErrorDetail("password", "비밀번호는 8자 이상이어야 합니다."))
            );
        }
    }
}
