package com.jiber.backend.property;

public record AdministrativeClusterResponse(
        String clusterId,
        AdministrativeClusterLevel level,
        String sido,
        String sigungu,
        String legalDong,
        String label,
        Double centerLat,
        Double centerLng,
        Integer propertyCount,
        Integer transactionCount,
        Long averageDealAmount
) {
}
