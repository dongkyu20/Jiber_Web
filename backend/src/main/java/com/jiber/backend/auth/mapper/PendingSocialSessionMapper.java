package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PendingSocialSessionMapper {

    int insert(PendingSocialSessionInsertCommand command);

    PendingSocialSessionRecord findByTokenHash(@Param("pendingTokenHash") String pendingTokenHash);

    PendingSocialSessionRecord findActiveByTokenHash(
            @Param("pendingTokenHash") String pendingTokenHash,
            @Param("now") OffsetDateTime now
    );

    int consume(
            @Param("pendingTokenHash") String pendingTokenHash,
            @Param("consumedAt") OffsetDateTime consumedAt
    );
}
