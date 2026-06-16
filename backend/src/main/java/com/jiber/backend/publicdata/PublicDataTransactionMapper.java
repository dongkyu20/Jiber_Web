package com.jiber.backend.publicdata;

import com.jiber.backend.property.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class PublicDataTransactionMapper {

    private final SourceKeyGenerator sourceKeyGenerator;

    public PublicDataTransactionMapper(SourceKeyGenerator sourceKeyGenerator) {
        this.sourceKeyGenerator = sourceKeyGenerator;
    }

    public ImportedApartmentTransaction toImportedTransaction(PublicDataApartmentItem item, PublicDataApiType apiType) {
        var transactionType = transactionType(item, apiType);
        return new ImportedApartmentTransaction(
                sourceKeyGenerator.generate(item, transactionType),
                transactionType,
                item.lawdCd(),
                item.legalDong(),
                item.jibun(),
                item.apartmentName(),
                item.exclusiveAreaM2(),
                item.floor(),
                item.builtYear(),
                item.dealDate(),
                item.dealAmountKrw(),
                item.depositAmountKrw(),
                item.monthlyRentKrw() == null ? 0L : item.monthlyRentKrw()
        );
    }

    private TransactionType transactionType(PublicDataApartmentItem item, PublicDataApiType apiType) {
        if (apiType == PublicDataApiType.SALE) {
            return TransactionType.SALE;
        }
        var monthlyRent = item.monthlyRentKrw() == null ? 0L : item.monthlyRentKrw();
        return monthlyRent > 0 ? TransactionType.MONTHLY_RENT : TransactionType.JEONSE;
    }
}
