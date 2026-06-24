package com.jiber.backend.property.dto;

import java.util.List;

public record PropertyMapResponse(
        List<PropertyMapItemResponse> items,
        List<AdministrativeClusterResponse> administrativeClusters,
        BoundsResponse bounds,
        MapFilterResponse filters
) {
}
