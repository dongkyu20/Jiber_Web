package com.jiber.backend.notice.dto;

import com.jiber.backend.notice.dto.*;
import com.jiber.backend.notice.service.*;

public record NoticeMutationResponse(
        Long noticeId,
        String message
) {
}
