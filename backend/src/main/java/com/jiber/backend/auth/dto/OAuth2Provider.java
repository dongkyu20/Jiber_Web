package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.util.Locale;

public enum OAuth2Provider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuth2Provider fromRegistrationId(String registrationId) {
        return OAuth2Provider.valueOf(registrationId.toUpperCase(Locale.ROOT));
    }
}
