package com.jiber.backend.auth.mapper;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.time.OffsetDateTime;

public record RefreshSessionInsertCommand(
        Long userId,
        String refreshTokenHash,
        Long rotatedFromSessionId,
        String userAgent,
        byte[] ipAddress,
        OffsetDateTime expiresAt
) {
}
