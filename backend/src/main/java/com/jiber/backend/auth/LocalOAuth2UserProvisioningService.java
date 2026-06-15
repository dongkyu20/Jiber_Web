package com.jiber.backend.auth;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalOAuth2UserProvisioningService {

    private final AuthUserMapper authUserMapper;
    private final Clock clock;

    public LocalOAuth2UserProvisioningService(AuthUserMapper authUserMapper) {
        this(authUserMapper, Clock.systemUTC());
    }

    LocalOAuth2UserProvisioningService(AuthUserMapper authUserMapper, Clock clock) {
        this.authUserMapper = authUserMapper;
        this.clock = clock;
    }

    public AuthUserPrincipal provision(OAuth2ProviderUser providerUser) {
        validate(providerUser);
        authUserMapper.upsertOAuthUser(
                providerUser.provider().name(),
                providerUser.providerUserId(),
                providerUser.email(),
                providerUser.displayName(),
                OffsetDateTime.now(clock)
        );
        var user = authUserMapper.findByProvider(providerUser.provider().name(), providerUser.providerUserId());
        if (user == null || Boolean.FALSE.equals(user.enabled())) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
        }
        return user.toPrincipal();
    }

    private void validate(OAuth2ProviderUser providerUser) {
        if (providerUser == null || providerUser.provider() == null || !StringUtils.hasText(providerUser.providerUserId())) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
        }
    }
}
