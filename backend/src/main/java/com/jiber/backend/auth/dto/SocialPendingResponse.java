package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

public record SocialPendingResponse(
        String provider,
        String email,
        String displayName,
        boolean matchingEmailAccountExists
) {
}
