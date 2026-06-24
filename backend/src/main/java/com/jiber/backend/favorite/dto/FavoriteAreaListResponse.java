package com.jiber.backend.favorite.dto;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import java.util.List;

public record FavoriteAreaListResponse(
        List<FavoriteAreaItemResponse> items
) {
}
