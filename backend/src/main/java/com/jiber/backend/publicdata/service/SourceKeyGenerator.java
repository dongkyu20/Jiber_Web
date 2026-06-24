package com.jiber.backend.publicdata.service;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import com.jiber.backend.property.dto.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class SourceKeyGenerator {

    public String generate(PublicDataApartmentItem item, PublicDataApiType apiType, TransactionType transactionType) {
        return String.join("|",
                "PUBLIC_DATA",
                apiType.sourcePrefix(),
                transactionType.name(),
                safe(item.lawdCd()),
                item.dealDate() == null ? "" : item.dealDate().toString(),
                safe(item.legalDong()),
                safe(item.jibun()),
                safe(item.apartmentName()),
                item.exclusiveAreaM2() == null ? "" : item.exclusiveAreaM2().stripTrailingZeros().toPlainString(),
                item.floor() == null ? "" : item.floor().toString(),
                safe(item.sourceSequence())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
