package com.jiber.backend.property.client;

import com.jiber.backend.property.dto.ShapRequest;
import com.jiber.backend.property.dto.ShapResponse;
import com.jiber.backend.property.dto.ValuationRequest;
import com.jiber.backend.property.dto.ValuationResponse;
import com.jiber.backend.property.dto.NewApartmentAnalysisRequest;
import com.jiber.backend.property.mapper.PropertyDetailRow;

public interface PropertyValuationClient {

    ValuationResponse valuateApartment(PropertyDetailRow property, ValuationRequest request);

    ShapResponse explainApartment(PropertyDetailRow property, ShapRequest request);

    default ValuationResponse valuateNewApartment(NewApartmentAnalysisRequest request) {
        throw new UnsupportedOperationException("New apartment valuation is not implemented.");
    }

    default ShapResponse explainNewApartment(NewApartmentAnalysisRequest request) {
        throw new UnsupportedOperationException("New apartment SHAP is not implemented.");
    }
}
