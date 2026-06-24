package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

public record FavoriteApartmentDeleteResponse(
        Long propertyId,
        String message
) {
}
