package com.jiber.backend.property.dto;

import com.jiber.backend.common.PageMetadata;
import java.util.List;

public record PropertySearchResponse(
        List<PropertySearchItemResponse> items,
        PageMetadata page
) {
}
