package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import com.jiber.backend.property.dto.TransactionType;
import com.jiber.backend.property.dto.PropertyType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedApartmentTransaction(
        String sourceKey,
        PropertyType propertyType,
        TransactionType transactionType,
        String lawdCd,
        String legalDong,
        String jibun,
        String apartmentName,
        BigDecimal exclusiveAreaM2,
        Integer floor,
        Integer builtYear,
        LocalDate dealDate,
        Long dealAmountKrw,
        Long depositAmountKrw,
        Long monthlyRentKrw
) {
}
