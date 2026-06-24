package com.jiber.backend.property.client;

import java.math.BigDecimal;

public record ModelServerApartmentFeatures(
        String sido,
        String sigungu,
        String legalDong,
        String propertyName,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer householdCount,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Integer builtYear,
        Integer dealYear,
        Integer dealMonth,
        BigDecimal distanceToStationM
) {
}
