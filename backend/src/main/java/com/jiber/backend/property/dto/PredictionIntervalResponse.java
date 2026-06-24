package com.jiber.backend.property.dto;

public record PredictionIntervalResponse(
        Long lower,
        Long upper
) {
}
