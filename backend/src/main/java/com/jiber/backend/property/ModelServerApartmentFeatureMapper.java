package com.jiber.backend.property;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ModelServerApartmentFeatureMapper {

    private static final BigDecimal DEFAULT_DISTANCE_TO_STATION_M = new BigDecimal("420");

    public ModelServerApartmentInferenceRequest toInternalRequest(PropertyDetailRow property, ValuationRequest request) {
        return toInternalRequest(property, request.exclusiveAreaM2(), request.floor(), request.asOfDate());
    }

    public ModelServerApartmentInferenceRequest toInternalRequest(PropertyDetailRow property, ShapRequest request) {
        return toInternalRequest(property, request.exclusiveAreaM2(), request.floor(), request.asOfDate());
    }

    private ModelServerApartmentInferenceRequest toInternalRequest(
            PropertyDetailRow property,
            BigDecimal exclusiveAreaM2,
            Integer floor,
            LocalDate asOfDate
    ) {
        var features = new ModelServerApartmentFeatures(
                property.getSido(),
                property.getSigungu(),
                property.getLegalDong(),
                property.getName(),
                property.getLatitude(),
                property.getLongitude(),
                property.getHouseholdCount(),
                exclusiveAreaM2,
                floor,
                property.getBuiltYear(),
                asOfDate.getYear(),
                asOfDate.getMonthValue(),
                DEFAULT_DISTANCE_TO_STATION_M
        );
        return new ModelServerApartmentInferenceRequest(property.getPropertyId(), asOfDate.toString(), features);
    }
}
