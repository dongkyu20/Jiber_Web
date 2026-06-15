package com.jiber.backend.auth;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthUserMapper authUserMapper;

    public AuthService(
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authUserMapper = authUserMapper;
    }

    public AuthMeResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return AuthMeResponse.unauthenticated();
        }
        return AuthMeResponse.authenticated(principal);
    }

    AuthRefreshResult refresh(String rawRefreshToken, RefreshRequestContext context) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw authRequired();
        }
        var rotation = refreshTokenService.rotate(rawRefreshToken, context);
        var user = authUserMapper.findById(rotation.userId());
        if (user == null || Boolean.FALSE.equals(user.enabled())) {
            throw authRequired();
        }
        var principal = user.toPrincipal();
        var accessToken = jwtTokenService.issueAccessToken(principal);
        return new AuthRefreshResult(AuthTokenResponse.of(accessToken, principal), rotation.token());
    }

    void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private ApiException authRequired() {
        return new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
    }
}
