package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import com.jiber.backend.property.PropertyType;
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
    boolean hasSuccessfulGeocoding() {
        return geocodingStatus == GeocodingStatus.SUCCESS
                && geocodingCacheStatus == GeocodingStatus.SUCCESS
                && latitude != null
                && longitude != null;
    }
}
