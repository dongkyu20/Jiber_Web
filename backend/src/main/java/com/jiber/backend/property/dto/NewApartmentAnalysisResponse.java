package com.jiber.backend.property.dto;

public record NewApartmentAnalysisResponse(
        String propertyName,
        ValuationResponse valuation,
        ShapResponse shap,
        String message
) {
}
