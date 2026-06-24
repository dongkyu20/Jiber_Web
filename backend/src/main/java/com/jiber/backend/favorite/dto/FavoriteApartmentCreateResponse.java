package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import java.time.OffsetDateTime;

public record FavoriteApartmentCreateResponse(
        Long favoriteId,
        Long propertyId,
        OffsetDateTime createdAt,
        String message
) {
}
