package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void rejectsShortCredential() {
        var policy = new PasswordPolicy();

        assertThatThrownBy(() -> policy.validate("short-1"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
