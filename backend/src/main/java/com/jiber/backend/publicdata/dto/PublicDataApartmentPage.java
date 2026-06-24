package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

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
