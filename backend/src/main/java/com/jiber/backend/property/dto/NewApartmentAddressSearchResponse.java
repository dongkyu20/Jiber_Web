package com.jiber.backend.property.dto;

import java.math.BigDecimal;

public record NewApartmentAddressSearchResponse(
        String fullAddress,
        String roadAddress,
        String jibunAddress,
        String sido,
        String sigungu,
        String legalDong,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
