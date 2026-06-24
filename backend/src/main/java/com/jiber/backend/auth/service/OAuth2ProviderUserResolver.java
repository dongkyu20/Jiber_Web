package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ProviderUserResolver {

    public OAuth2ProviderUser resolve(String registrationId, OAuth2User oauth2User) {
        var provider = OAuth2Provider.fromRegistrationId(registrationId);
        return switch (provider) {
            case GOOGLE -> google(oauth2User.getAttributes());
            case KAKAO -> kakao(oauth2User.getAttributes());
            case NAVER -> naver(oauth2User.getAttributes());
        };
    }

    private OAuth2ProviderUser google(Map<String, Object> attributes) {
        return new OAuth2ProviderUser(
                OAuth2Provider.GOOGLE,
                string(attributes.get("sub")),
                string(attributes.get("email")),
                string(attributes.get("name"))
        );
    }

    private OAuth2ProviderUser kakao(Map<String, Object> attributes) {
        var account = valueAsMap(attributes.get("kakao_account"));
        var profile = valueAsMap(account.get("profile"));
        return new OAuth2ProviderUser(
                OAuth2Provider.KAKAO,
                string(attributes.get("id")),
                string(account.get("email")),
                string(profile.get("nickname"))
        );
    }

    private OAuth2ProviderUser naver(Map<String, Object> attributes) {
        var response = valueAsMap(attributes.get("response"));
        return new OAuth2ProviderUser(
                OAuth2Provider.NAVER,
                string(response.get("id")),
                string(response.get("email")),
                firstText(response.get("name"), response.get("nickname"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> valueAsMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String firstText(Object first, Object second) {
        var firstValue = string(first);
        return firstValue == null ? string(second) : firstValue;
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        var text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
