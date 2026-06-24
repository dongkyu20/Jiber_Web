package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FavoriteApartmentCreateRequest(
        @NotNull @Positive Long propertyId
) {
}
