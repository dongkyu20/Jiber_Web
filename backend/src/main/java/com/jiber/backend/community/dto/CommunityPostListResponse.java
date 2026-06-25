package com.jiber.backend.community.dto;

import com.jiber.backend.common.PageMetadata;
import java.util.List;

public record CommunityPostListResponse(
        List<CommunityPostSummaryResponse> items,
        PageMetadata page
) {
}
