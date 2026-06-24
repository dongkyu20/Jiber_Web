package com.jiber.backend.auth.dto;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.util.Set;

public record AuthUserPrincipal(
        Long userId,
        String email,
        String displayName,
        Set<String> roles
) {
}
