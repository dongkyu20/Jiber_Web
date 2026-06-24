package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import com.jiber.backend.property.dto.TransactionType;
import com.jiber.backend.property.dto.PropertyType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CanonicalApartmentRawRow(
        Long rawTransactionId,
        String sourceKey,
        PropertyType propertyType,
        TransactionType transactionType,
        String sido,
        String sigungu,
        String legalDong,
        String jibun,
        String fullAddress,
        String apartmentName,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Integer builtYear,
        LocalDate dealDate,
        Long dealAmountKrw,
        Long depositAmountKrw,
        Long monthlyRentKrw,
        GeocodingStatus geocodingStatus,
        GeocodingStatus geocodingCacheStatus,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public boolean hasSuccessfulGeocoding() {
        return geocodingStatus == GeocodingStatus.SUCCESS
                && geocodingCacheStatus == GeocodingStatus.SUCCESS
                && latitude != null
                && longitude != null;
    }
}
