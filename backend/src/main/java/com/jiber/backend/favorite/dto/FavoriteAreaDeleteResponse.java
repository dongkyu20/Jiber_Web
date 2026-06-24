package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

public record FavoriteAreaDeleteResponse(
        Long favoriteAreaId,
        String message
) {
}
