package com.jiber.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2ProviderUserResolver providerUserResolver;
    private final SocialLoginService socialLoginService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final PendingSocialCookieService pendingSocialCookieService;
    private final FrontendProperties frontendProperties;

    public OAuth2LoginSuccessHandler(
            OAuth2ProviderUserResolver providerUserResolver,
            SocialLoginService socialLoginService,
            RefreshTokenCookieService refreshTokenCookieService,
            PendingSocialCookieService pendingSocialCookieService,
            FrontendProperties frontendProperties
    ) {
        this.providerUserResolver = providerUserResolver;
        this.socialLoginService = socialLoginService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.pendingSocialCookieService = pendingSocialCookieService;
        this.frontendProperties = frontendProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var providerUser = providerUserResolver.resolve(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getPrincipal()
        );
        var result = socialLoginService.handleOAuthSuccess(providerUser, RefreshRequestContext.from(request));

        if (result.type() == SocialLoginResult.Type.LINKED) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.authResult().refreshToken()).toString());
            response.sendRedirect(frontendCallbackUrl());
            return;
        }

        response.addHeader(HttpHeaders.SET_COOKIE, pendingSocialCookieService.createPendingCookie(result.pendingToken().token()).toString());
        response.sendRedirect(frontendSocialSignupUrl());
    }

    private String frontendCallbackUrl() {
        return UriComponentsBuilder.fromUriString(frontendProperties.publicBaseUrl())
                .path("/login/callback")
                .build()
                .toUriString();
    }

    private String frontendSocialSignupUrl() {
        return UriComponentsBuilder.fromUriString(frontendProperties.publicBaseUrl())
                .path("/signup/social")
                .build()
                .toUriString();
    }
}
