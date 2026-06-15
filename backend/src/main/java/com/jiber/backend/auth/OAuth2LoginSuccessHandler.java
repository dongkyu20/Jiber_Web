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
    private final LocalOAuth2UserProvisioningService userProvisioningService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final FrontendProperties frontendProperties;

    public OAuth2LoginSuccessHandler(
            OAuth2ProviderUserResolver providerUserResolver,
            LocalOAuth2UserProvisioningService userProvisioningService,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieService refreshTokenCookieService,
            FrontendProperties frontendProperties
    ) {
        this.providerUserResolver = providerUserResolver;
        this.userProvisioningService = userProvisioningService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieService = refreshTokenCookieService;
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
        var localPrincipal = userProvisioningService.provision(providerUser);
        var refreshToken = refreshTokenService.issue(localPrincipal.userId(), RefreshRequestContext.from(request));

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(refreshToken.token()).toString());
        response.sendRedirect(frontendCallbackUrl());
    }

    private String frontendCallbackUrl() {
        return UriComponentsBuilder.fromUriString(frontendProperties.publicBaseUrl())
                .path("/login/callback")
                .build()
                .toUriString();
    }
}
