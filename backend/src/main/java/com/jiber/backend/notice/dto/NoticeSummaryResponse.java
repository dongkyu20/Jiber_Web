package com.jiber.backend.notice.dto;

import com.jiber.backend.notice.dto.*;
import com.jiber.backend.notice.service.*;

import java.time.OffsetDateTime;

public record NoticeSummaryResponse(
        Long noticeId,
        String title,
        String summary,
        boolean pinned,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {
}
