package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CanonicalApartmentTransactionCommand(
        Long propertyId,
        TransactionType transactionType,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Long dealAmountKrw,
        Long depositAmountKrw,
        Long monthlyRentKrw,
        LocalDate dealDate,
        String sourceSystem,
        String sourceTransactionId
) {
    public static CanonicalApartmentTransactionCommand from(Long propertyId, CanonicalApartmentRawRow row) {
        return new CanonicalApartmentTransactionCommand(
                propertyId,
                row.transactionType(),
                row.exclusiveAreaM2(),
                row.floor(),
                row.dealAmountKrw(),
                row.depositAmountKrw(),
                row.monthlyRentKrw(),
                row.dealDate(),
                CanonicalApartmentUpsertService.SOURCE_SYSTEM,
                row.sourceKey()
        );
    }
}
