package com.jiber.backend.notice.dto;

import com.jiber.backend.notice.dto.*;
import com.jiber.backend.notice.service.*;

import com.jiber.backend.common.PageMetadata;
import java.util.List;

public record NoticeListResponse(
        List<NoticeSummaryResponse> items,
        PageMetadata page
) {
}
