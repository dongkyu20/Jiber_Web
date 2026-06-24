package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.dto.PropertyType;
import org.junit.jupiter.api.Test;

class PublicDataApiTypeTest {

    @Test
    void saleUsesApartmentTradeEndpointAvailableForTheConfiguredPublicDataKey() {
        assertThat(PublicDataApiType.SALE.endpointPath())
                .isEqualTo("/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade");
    }

    @Test
    void officetelAndVillaApiTypesExposePublicDataEndpointsAndPropertyTypes() {
        assertThat(PublicDataApiType.OFFICETEL_SALE.endpointPath())
                .isEqualTo("/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade");
        assertThat(PublicDataApiType.OFFICETEL_SALE.propertyType()).isEqualTo(PropertyType.OFFICETEL);
        assertThat(PublicDataApiType.OFFICETEL_SALE.isSale()).isTrue();

        assertThat(PublicDataApiType.OFFICETEL_RENT.endpointPath())
                .isEqualTo("/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent");
        assertThat(PublicDataApiType.OFFICETEL_RENT.propertyType()).isEqualTo(PropertyType.OFFICETEL);
        assertThat(PublicDataApiType.OFFICETEL_RENT.isSale()).isFalse();

        assertThat(PublicDataApiType.VILLA_SALE.endpointPath())
                .isEqualTo("/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade");
        assertThat(PublicDataApiType.VILLA_SALE.propertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(PublicDataApiType.VILLA_SALE.isSale()).isTrue();

        assertThat(PublicDataApiType.VILLA_RENT.endpointPath())
                .isEqualTo("/1613000/RTMSDataSvcRHRent/getRTMSDataSvcRHRent");
        assertThat(PublicDataApiType.VILLA_RENT.propertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(PublicDataApiType.VILLA_RENT.isSale()).isFalse();
    }
}
