package com.jiber.backend.publicdata;

import java.util.List;

public record PublicDataApartmentPage(
        int pageNo,
        int numOfRows,
        int totalCount,
        List<PublicDataApartmentItem> items
) {
    public boolean hasNextPage() {
        return pageNo * numOfRows < totalCount;
    }
}
