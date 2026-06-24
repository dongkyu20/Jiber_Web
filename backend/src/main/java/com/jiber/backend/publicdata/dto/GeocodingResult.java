package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import java.math.BigDecimal;

public record GeocodingResult(
        String query,
        GeocodingStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        String failureReason
) {
    public static GeocodingResult success(String query, BigDecimal latitude, BigDecimal longitude) {
        return new GeocodingResult(query, GeocodingStatus.SUCCESS, latitude, longitude, null);
    }

    public static GeocodingResult failure(String query, GeocodingStatus status, String reason) {
        return new GeocodingResult(query, status, null, null, reason);
    }
}
