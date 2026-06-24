package com.jiber.backend.publicdata.mapper;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import java.math.BigDecimal;

public record GeocodingCacheRecord(
        String addressKey,
        String fullAddress,
        GeocodingStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        String failureReason
) {
    public static GeocodingCacheRecord from(NormalizedAddress address, GeocodingResult result) {
        return new GeocodingCacheRecord(
                address.addressKey(),
                address.fullAddress(),
                result.status(),
                result.latitude(),
                result.longitude(),
                result.failureReason()
        );
    }
}
