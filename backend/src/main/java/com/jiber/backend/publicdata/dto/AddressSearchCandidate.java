package com.jiber.backend.publicdata.dto;

import java.math.BigDecimal;

public record AddressSearchCandidate(
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
