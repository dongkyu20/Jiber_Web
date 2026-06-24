package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

public record OAuth2ProviderUser(
        OAuth2Provider provider,
        String providerUserId,
        String email,
        String displayName
) {
}
