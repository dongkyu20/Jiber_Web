package com.jiber.backend.publicdata;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicDataApartmentItem(
        String lawdCd,
        String legalDong,
        String jibun,
        String apartmentName,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Integer builtYear,
        LocalDate dealDate,
        Long dealAmountKrw,
        Long depositAmountKrw,
        Long monthlyRentKrw,
        String sourceSequence
) {
}
