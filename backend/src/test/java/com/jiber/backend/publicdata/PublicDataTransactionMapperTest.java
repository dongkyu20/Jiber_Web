package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PublicDataTransactionMapperTest {

    private final PublicDataTransactionMapper mapper = new PublicDataTransactionMapper(new SourceKeyGenerator());

    @Test
    void mapsSaleApiItemToSaleTransaction() {
        var transaction = mapper.toImportedTransaction(item(1250000000L, null, null), PublicDataApiType.SALE);

        assertThat(transaction.transactionType()).isEqualTo(TransactionType.SALE);
        assertThat(transaction.dealAmountKrw()).isEqualTo(1250000000L);
    }

    @Test
    void mapsRentApiItemWithoutMonthlyRentToJeonse() {
        var transaction = mapper.toImportedTransaction(item(null, 600000000L, 0L), PublicDataApiType.RENT);

        assertThat(transaction.transactionType()).isEqualTo(TransactionType.JEONSE);
        assertThat(transaction.depositAmountKrw()).isEqualTo(600000000L);
        assertThat(transaction.monthlyRentKrw()).isZero();
    }

    @Test
    void mapsRentApiItemWithMonthlyRentToMonthlyRent() {
        var transaction = mapper.toImportedTransaction(item(null, 50000000L, 1200000L), PublicDataApiType.RENT);

        assertThat(transaction.transactionType()).isEqualTo(TransactionType.MONTHLY_RENT);
        assertThat(transaction.depositAmountKrw()).isEqualTo(50000000L);
        assertThat(transaction.monthlyRentKrw()).isEqualTo(1200000L);
    }

    private PublicDataApartmentItem item(Long dealAmount, Long depositAmount, Long monthlyRent) {
        return new PublicDataApartmentItem(
                "11680",
                "역삼동",
                "12-3",
                "예시아파트",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                dealAmount,
                depositAmount,
                monthlyRent,
                "1"
        );
    }
}
