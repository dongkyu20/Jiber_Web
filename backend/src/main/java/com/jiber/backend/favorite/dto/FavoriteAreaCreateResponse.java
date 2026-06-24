package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import java.time.OffsetDateTime;

public record FavoriteAreaCreateResponse(
        Long favoriteAreaId,
        String label,
        OffsetDateTime createdAt,
        String message
) {
}
