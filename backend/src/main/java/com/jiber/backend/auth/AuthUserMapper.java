package com.jiber.backend.auth;

import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

    AuthUserRecord findById(@Param("userId") Long userId);

    AuthUserRecord findByProvider(
            @Param("oauthProvider") String oauthProvider,
            @Param("providerUserId") String providerUserId
    );

    int upsertOAuthUser(
            @Param("oauthProvider") String oauthProvider,
            @Param("providerUserId") String providerUserId,
            @Param("email") String email,
            @Param("displayName") String displayName,
            @Param("lastLoginAt") OffsetDateTime lastLoginAt
    );
}
