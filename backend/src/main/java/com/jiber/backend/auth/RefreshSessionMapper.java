package com.jiber.backend.auth;

import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshSessionMapper {

    int insert(RefreshSessionInsertCommand command);

    RefreshSessionRecord findByTokenHash(@Param("refreshTokenHash") String refreshTokenHash);

    RefreshSessionRecord findActiveByTokenHash(
            @Param("refreshTokenHash") String refreshTokenHash,
            @Param("now") OffsetDateTime now
    );

    int revokeBySessionId(
            @Param("refreshSessionId") Long refreshSessionId,
            @Param("revokedAt") OffsetDateTime revokedAt
    );

    int revokeByTokenHash(
            @Param("refreshTokenHash") String refreshTokenHash,
            @Param("revokedAt") OffsetDateTime revokedAt
    );

    int revokeSessionFamily(
            @Param("refreshSessionId") Long refreshSessionId,
            @Param("revokedAt") OffsetDateTime revokedAt
    );
}
