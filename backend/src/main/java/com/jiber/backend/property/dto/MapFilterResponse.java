package com.jiber.backend.property.dto;

import java.util.List;

public record MapFilterResponse(
        List<PropertyType> propertyTypes,
        List<TransactionType> transactionTypes,
        Integer zoomLevel
) {
}
