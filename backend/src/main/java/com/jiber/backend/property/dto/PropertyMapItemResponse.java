package com.jiber.backend.property.dto;

public record PropertyMapItemResponse(
        Long propertyId,
        PropertyType propertyType,
        String name,
        String address,
        Double lat,
        Double lng,
        LatestTransactionResponse latestTransaction,
        Integer dealCount,
        Integer recentTransactionCount,
        Long recentYearAverageDealAmount,
        Long recentYearAverageJeonseDepositAmount,
        boolean aiAvailable
) {
}
