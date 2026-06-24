package com.jiber.backend.property.client;

public record ModelServerShapValue(
        String feature,
        String labelKo,
        Double value,
        Long shapValue,
        String direction
) {
}
