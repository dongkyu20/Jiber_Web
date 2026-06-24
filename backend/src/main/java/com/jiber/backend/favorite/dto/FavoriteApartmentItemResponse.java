package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import com.jiber.backend.property.dto.LatestTransactionResponse;
import com.jiber.backend.property.dto.PropertyType;
import java.time.OffsetDateTime;

public record FavoriteApartmentItemResponse(
        Long favoriteId,
        Long propertyId,
        PropertyType propertyType,
        String name,
        String address,
        Double lat,
        Double lng,
        LatestTransactionResponse latestTransaction,
        OffsetDateTime createdAt
) {
}
