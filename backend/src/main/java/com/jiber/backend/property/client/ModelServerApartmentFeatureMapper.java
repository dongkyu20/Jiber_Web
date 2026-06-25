package com.jiber.backend.property.client;

import com.jiber.backend.property.dto.ShapRequest;
import com.jiber.backend.property.dto.ValuationRequest;
import com.jiber.backend.property.dto.NewApartmentAnalysisRequest;
import com.jiber.backend.property.mapper.PropertyDetailRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ModelServerApartmentFeatureMapper {

    public ModelServerApartmentInferenceRequest toInternalRequest(PropertyDetailRow property, ValuationRequest request) {
        return toInternalRequest(property, request.exclusiveAreaM2(), request.floor(), request.asOfDate());
    }

    public ModelServerApartmentInferenceRequest toInternalRequest(PropertyDetailRow property, ShapRequest request) {
        return toInternalRequest(property, request.exclusiveAreaM2(), request.floor(), request.asOfDate());
    }

    public ModelServerApartmentInferenceRequest toInternalRequest(NewApartmentAnalysisRequest request) {
        var features = new ModelServerApartmentFeatures(
                request.sido(),
                request.sigungu(),
                request.legalDong(),
                request.propertyName(),
                request.latitude(),
                request.longitude(),
                request.householdCount(),
                request.exclusiveAreaM2(),
                request.floor(),
                request.builtYear(),
                request.asOfDate().getYear(),
                request.asOfDate().getMonthValue(),
                request.distanceToStationM()
        );
        return new ModelServerApartmentInferenceRequest(0L, request.asOfDate().toString(), features);
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
                null
        );
        return new ModelServerApartmentInferenceRequest(property.getPropertyId(), asOfDate.toString(), features);
    }
}
