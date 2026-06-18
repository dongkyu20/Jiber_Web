package com.jiber.backend.auth;

public record SocialLoginResult(
        Type type,
        AuthRefreshResult authResult,
        IssuedPendingSocialToken pendingToken
) {
    public enum Type {
        LINKED,
        PENDING
    }

    public static SocialLoginResult linked(AuthRefreshResult authResult) {
        return new SocialLoginResult(Type.LINKED, authResult, null);
    }

    public static SocialLoginResult pending(IssuedPendingSocialToken pendingToken) {
        return new SocialLoginResult(Type.PENDING, null, pendingToken);
    }
}
