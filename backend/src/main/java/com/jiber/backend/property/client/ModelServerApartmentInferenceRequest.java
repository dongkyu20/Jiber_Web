package com.jiber.backend.property.client;

public record ModelServerApartmentInferenceRequest(
        Long propertyId,
        String asOfDate,
        ModelServerApartmentFeatures features
) {
}
