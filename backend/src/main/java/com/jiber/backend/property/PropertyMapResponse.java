package com.jiber.backend.property;

import java.util.List;

public record PropertyMapResponse(
        List<PropertyMapItemResponse> items,
        List<AdministrativeClusterResponse> administrativeClusters,
        BoundsResponse bounds,
        MapFilterResponse filters
) {
}
