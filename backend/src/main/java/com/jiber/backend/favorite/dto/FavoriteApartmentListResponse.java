package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import java.util.List;

public record FavoriteApartmentListResponse(
        List<FavoriteApartmentItemResponse> items
) {
}
