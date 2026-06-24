package com.jiber.backend.property;

public interface PropertyValuationClient {

    ValuationResponse valuateApartment(PropertyDetailRow property, ValuationRequest request);

    ShapResponse explainApartment(PropertyDetailRow property, ShapRequest request);
}
