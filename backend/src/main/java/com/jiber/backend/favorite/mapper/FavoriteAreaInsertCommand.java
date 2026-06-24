package com.jiber.backend.favorite.mapper;

import com.jiber.backend.favorite.dto.*;
import com.jiber.backend.favorite.mapper.*;
import com.jiber.backend.favorite.service.*;

import java.math.BigDecimal;

public record FavoriteAreaInsertCommand(
        Long userId,
        String label,
        String sido,
        String sigungu,
        String legalDong,
        BigDecimal centerLat,
        BigDecimal centerLng,
        Integer zoomLevel,
        String normalizedKey
) {
}
