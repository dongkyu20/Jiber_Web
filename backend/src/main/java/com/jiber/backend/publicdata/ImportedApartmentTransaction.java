package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedApartmentTransaction(
        String sourceKey,
        TransactionType transactionType,
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
        Long monthlyRentKrw
) {
}
