package com.jiber.backend.auth.service;

import com.jiber.backend.auth.*;
import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.common.error.ErrorDetail;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String IDENTIFIER_RECOVERY_MESSAGE =
            "집er 아이디는 가입 이메일입니다. 보안상 화면에는 가입 이메일을 표시하지 않습니다.";
    private static final String PASSWORD_RECOVERY_MESSAGE =
            "입력한 이메일이 가입되어 있다면 비밀번호 재설정 안내를 받을 수 있습니다. 현재 메일 발송 연동 전에는 관리자에게 문의해 주세요.";
    private static final String DIRECT_PASSWORD_RESET_MESSAGE =
            "입력한 정보가 가입 정보와 일치하면 비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.";

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    @Autowired
    public AuthService(
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy
    ) {
        this(jwtTokenService, refreshTokenService, authUserMapper, passwordEncoder, emailNormalizer, passwordPolicy, Clock.systemUTC());
    }

    public AuthService(
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper
    ) {
        this(
                jwtTokenService,
                refreshTokenService,
                authUserMapper,
                new BCryptPasswordEncoder(),
                new EmailNormalizer(),
                new PasswordPolicy(),
                Clock.systemUTC()
        );
    }

    public static AuthService forTesting(
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            Clock clock
    ) {
        return new AuthService(jwtTokenService, refreshTokenService, authUserMapper, passwordEncoder, emailNormalizer, passwordPolicy, clock);
    }

    private AuthService(
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            Clock clock
    ) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authUserMapper = authUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    public AuthMeResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return AuthMeResponse.unauthenticated();
        }
        return AuthMeResponse.authenticated(principal);
    }

    public AuthRefreshResult signup(EmailSignupRequest request, RefreshRequestContext context) {
        var normalizedEmail = emailNormalizer.normalize(request.email());
        validateSignupEmail(normalizedEmail);
        passwordPolicy.validate(request.password());
        if (authUserMapper.findByEmail(normalizedEmail) != null) {
            throw emailAlreadyExists();
        }

        var now = OffsetDateTime.now(clock);
        try {
            authUserMapper.insertEmailUser(
                    normalizedEmail,
                    passwordEncoder.encode(request.password()),
                    request.displayName().trim(),
                    "USER",
                    true,
                    now
            );
        } catch (DuplicateKeyException exception) {
            throw emailAlreadyExists();
        }

        var user = authUserMapper.findByEmail(normalizedEmail);
        if (user == null || Boolean.FALSE.equals(user.enabled())) {
            throw authRequired();
        }
        return startSession(user, context);
    }

    public AuthRefreshResult login(EmailLoginRequest request, RefreshRequestContext context) {
        var normalizedEmail = emailNormalizer.normalize(request.email());
        var user = StringUtils.hasText(normalizedEmail) ? authUserMapper.findByEmail(normalizedEmail) : null;
        if (user == null || Boolean.FALSE.equals(user.enabled()) || !StringUtils.hasText(user.passwordHash())) {
            throw invalidCredentials();
        }
        if (!matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        authUserMapper.updateLastLoginAt(user.userId(), OffsetDateTime.now(clock));
        var updatedUser = authUserMapper.findById(user.userId());
        return startSession(updatedUser == null ? user : updatedUser, context);
    }

    public AccountRecoveryResponse recoverIdentifier(AccountIdentifierRecoveryRequest request) {
        return new AccountRecoveryResponse(IDENTIFIER_RECOVERY_MESSAGE);
    }

    public AccountRecoveryResponse requestPasswordRecovery(PasswordRecoveryRequest request) {
        return new AccountRecoveryResponse(PASSWORD_RECOVERY_MESSAGE);
    }

    public AccountRecoveryResponse directPasswordReset(DirectPasswordResetRequest request) {
        var normalizedEmail = emailNormalizer.normalize(request.email());
        passwordPolicy.validate(request.newPassword());
        var user = StringUtils.hasText(normalizedEmail) ? authUserMapper.findByEmail(normalizedEmail) : null;
        if (canResetPassword(user, request.displayName())) {
            var now = OffsetDateTime.now(clock);
            authUserMapper.updatePasswordHash(user.userId(), passwordEncoder.encode(request.newPassword()), now);
            refreshTokenService.revokeAllForUser(user.userId());
        }
        return new AccountRecoveryResponse(DIRECT_PASSWORD_RESET_MESSAGE);
    }

    public AuthUserResponse updateProfile(Authentication authentication, UpdateProfileRequest request) {
        var user = requireEnabledCurrentUser(authentication);
        var displayName = request.displayName().trim();
        var now = OffsetDateTime.now(clock);
        var changed = authUserMapper.updateDisplayName(user.userId(), displayName, now);
        if (changed == 0) {
            throw authRequired();
        }

        var updated = authUserMapper.findById(user.userId());
        if (updated == null || Boolean.FALSE.equals(updated.enabled())) {
            throw authRequired();
        }
        return AuthUserResponse.from(updated.toPrincipal());
    }

    public AccountMutationResponse changePassword(Authentication authentication, ChangePasswordRequest request) {
        var user = requireEnabledCurrentUser(authentication);
        verifyPassword(user, request.currentPassword());
        passwordPolicy.validate(request.newPassword());

        var now = OffsetDateTime.now(clock);
        var changed = authUserMapper.updatePasswordHash(user.userId(), passwordEncoder.encode(request.newPassword()), now);
        if (changed == 0) {
            throw authRequired();
        }
        refreshTokenService.revokeAllForUser(user.userId());
        return new AccountMutationResponse("비밀번호가 변경되었습니다. 다시 로그인해 주세요.");
    }

    public AccountMutationResponse deactivateAccount(Authentication authentication, DeactivateAccountRequest request) {
        var user = requireEnabledCurrentUser(authentication);
        verifyPassword(user, request.password());

        var now = OffsetDateTime.now(clock);
        var changed = authUserMapper.updateEnabled(user.userId(), false, now);
        if (changed == 0) {
            throw authRequired();
        }
        refreshTokenService.revokeAllForUser(user.userId());
        return new AccountMutationResponse("회원탈퇴가 완료되었습니다.");
    }

    public AuthRefreshResult refresh(String rawRefreshToken, RefreshRequestContext context) {
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

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthUserRecord requireEnabledCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            throw authRequired();
        }
        var user = authUserMapper.findById(principal.userId());
        if (user == null || Boolean.FALSE.equals(user.enabled())) {
            throw authRequired();
        }
        return user;
    }

    private void verifyPassword(AuthUserRecord user, String password) {
        if (user == null || !StringUtils.hasText(user.passwordHash()) || !matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
    }

    private ApiException authRequired() {
        return new ApiException(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.defaultMessage(), List.of());
    }

    private AuthRefreshResult startSession(AuthUserRecord user, RefreshRequestContext context) {
        var principal = user.toPrincipal();
        var refreshToken = refreshTokenService.issue(user.userId(), context);
        var accessToken = jwtTokenService.issueAccessToken(principal);
        return new AuthRefreshResult(AuthTokenResponse.of(accessToken, principal), refreshToken.token());
    }

    private boolean matches(String candidate, String passwordHash) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        try {
            return passwordEncoder.matches(candidate, passwordHash);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean canResetPassword(AuthUserRecord user, String displayName) {
        return user != null
                && Boolean.TRUE.equals(user.enabled())
                && StringUtils.hasText(user.passwordHash())
                && normalizeDisplayName(user.displayName()).equals(normalizeDisplayName(displayName));
    }

    private String normalizeDisplayName(String displayName) {
        return displayName == null ? "" : displayName.trim();
    }

    private void validateSignupEmail(String normalizedEmail) {
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.defaultMessage(),
                    List.of(new ErrorDetail("email", "이메일 형식이 올바르지 않습니다."))
            );
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.defaultMessage(), List.of());
    }

    private ApiException emailAlreadyExists() {
        return new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, ErrorCode.EMAIL_ALREADY_EXISTS.defaultMessage(), List.of());
    }
}
