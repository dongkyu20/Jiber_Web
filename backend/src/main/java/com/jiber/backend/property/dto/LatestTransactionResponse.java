package com.jiber.backend.property.dto;

import java.time.LocalDate;

public record LatestTransactionResponse(
        TransactionType transactionType,
        Long dealAmount,
        Long depositAmount,
        Long monthlyRent,
        LocalDate dealDate
) {
}
