package com.jiber.backend.auth;

import java.util.Locale;

public enum OAuth2Provider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuth2Provider fromRegistrationId(String registrationId) {
        return OAuth2Provider.valueOf(registrationId.toUpperCase(Locale.ROOT));
    }
}
