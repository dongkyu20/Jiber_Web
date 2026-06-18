package com.jiber.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication) {
        return authService.currentUser(authentication);
    }

    @PostMapping("/signup")
    public AuthTokenResponse signup(
            @Valid @RequestBody EmailSignupRequest signupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authService.signup(signupRequest, RefreshRequestContext.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString());
        return result.response();
    }

    @PostMapping("/login")
    public AuthTokenResponse login(
            @RequestBody EmailLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authService.login(loginRequest, RefreshRequestContext.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(
            @CookieValue(name = "${jiber.auth.refresh-token.cookie.name:JIBER_REFRESH_TOKEN}", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authService.refresh(refreshToken, RefreshRequestContext.from(request));
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString());
        return result.response();
    }

    @PostMapping("/logout")
    public AuthLogoutResponse logout(
            @CookieValue(name = "${jiber.auth.refresh-token.cookie.name:JIBER_REFRESH_TOKEN}", required = false) String refreshToken,
            @RequestBody(required = false) AuthLogoutRequest logoutRequest,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearRefreshCookie().toString());
        return new AuthLogoutResponse("로그아웃되었습니다.");
    }
}
