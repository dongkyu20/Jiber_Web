package com.jiber.backend.admin;

import com.jiber.backend.common.PageMetadata;
import java.util.List;

public record AdminUserListResponse(
        List<AdminUserSummaryResponse> items,
        PageMetadata page
) {
}
