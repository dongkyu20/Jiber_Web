package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

    AuthUserRecord findById(@Param("userId") Long userId);

    AuthUserRecord findByEmail(@Param("email") String email);

    int insertEmailUser(
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("displayName") String displayName,
            @Param("role") String role,
            @Param("enabled") Boolean enabled,
            @Param("lastLoginAt") OffsetDateTime lastLoginAt
    );

    int updateLastLoginAt(
            @Param("userId") Long userId,
            @Param("lastLoginAt") OffsetDateTime lastLoginAt
    );

    int updatePasswordHash(
            @Param("userId") Long userId,
            @Param("passwordHash") String passwordHash,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    int updateDisplayName(
            @Param("userId") Long userId,
            @Param("displayName") String displayName,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    int updateEnabled(
            @Param("userId") Long userId,
            @Param("enabled") Boolean enabled,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
