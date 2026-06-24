package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import com.jiber.backend.property.dto.PropertyType;

public enum PublicDataApiType {
    SALE("/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade", PropertyType.APARTMENT, true, "APT"),
    RENT("/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent", PropertyType.APARTMENT, false, "APT"),
    OFFICETEL_SALE("/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade", PropertyType.OFFICETEL, true, "OFFICETEL"),
    OFFICETEL_RENT("/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent", PropertyType.OFFICETEL, false, "OFFICETEL"),
    VILLA_SALE("/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade", PropertyType.VILLA, true, "VILLA"),
    VILLA_RENT("/1613000/RTMSDataSvcRHRent/getRTMSDataSvcRHRent", PropertyType.VILLA, false, "VILLA");

    private final String endpointPath;
    private final PropertyType propertyType;
    private final boolean sale;
    private final String sourcePrefix;

    PublicDataApiType(String endpointPath, PropertyType propertyType, boolean sale, String sourcePrefix) {
        this.endpointPath = endpointPath;
        this.propertyType = propertyType;
        this.sale = sale;
        this.sourcePrefix = sourcePrefix;
    }

    public String endpointPath() {
        return endpointPath;
    }

    public PropertyType propertyType() {
        return propertyType;
    }

    public boolean isSale() {
        return sale;
    }

    public String sourcePrefix() {
        return sourcePrefix;
    }
}
