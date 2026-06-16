package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class SourceKeyGenerator {

    public String generate(PublicDataApartmentItem item, TransactionType transactionType) {
        return String.join("|",
                "PUBLIC_DATA",
                "APT",
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
