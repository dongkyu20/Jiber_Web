package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import com.jiber.backend.property.PropertyType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicDataRawTransactionRecord(
        Long importRunId,
        String sourceKey,
        PropertyType propertyType,
        TransactionType transactionType,
        String lawdCd,
        String sido,
        String sigungu,
        String legalDong,
        String jibun,
        String addressKey,
        String fullAddress,
        String apartmentName,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Integer builtYear,
        LocalDate dealDate,
        Long dealAmountKrw,
        Long depositAmountKrw,
        Long monthlyRentKrw,
        String geocodingStatus
) {
    public static PublicDataRawTransactionRecord from(
            Long importRunId,
            LawdCode lawdCode,
            NormalizedAddress address,
            ImportedApartmentTransaction transaction
    ) {
        return from(importRunId, lawdCode, address, transaction, GeocodingStatus.PENDING);
    }

    public static PublicDataRawTransactionRecord from(
            Long importRunId,
            LawdCode lawdCode,
            NormalizedAddress address,
            ImportedApartmentTransaction transaction,
            GeocodingStatus geocodingStatus
    ) {
        return new PublicDataRawTransactionRecord(
                importRunId,
                transaction.sourceKey(),
                transaction.propertyType(),
                transaction.transactionType(),
                lawdCode.lawdCd(),
                lawdCode.sido(),
                lawdCode.sigungu(),
                transaction.legalDong(),
                transaction.jibun(),
                address.addressKey(),
                address.fullAddress(),
                transaction.apartmentName(),
                transaction.exclusiveAreaM2(),
                transaction.floor(),
                transaction.builtYear(),
                transaction.dealDate(),
                transaction.dealAmountKrw(),
                transaction.depositAmountKrw(),
                transaction.monthlyRentKrw(),
                geocodingStatus.name()
        );
    }
}
