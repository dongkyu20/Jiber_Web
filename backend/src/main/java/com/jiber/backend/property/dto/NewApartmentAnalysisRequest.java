package com.jiber.backend.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NewApartmentAnalysisRequest(
        @NotBlank @Size(max = 255) String propertyName,
        @NotBlank @Size(max = 100) String sido,
        @NotBlank @Size(max = 100) String sigungu,
        @NotBlank @Size(max = 100) String legalDong,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @PositiveOrZero Integer householdCount,
        @NotNull @DecimalMin("0.01") BigDecimal exclusiveAreaM2,
        @NotNull Integer floor,
        @Min(1) Integer topFloor,
        @NotNull @Min(1900) @Max(2100) Integer builtYear,
        @NotNull LocalDate asOfDate,
        @PositiveOrZero BigDecimal distanceToStationM
) {
}
