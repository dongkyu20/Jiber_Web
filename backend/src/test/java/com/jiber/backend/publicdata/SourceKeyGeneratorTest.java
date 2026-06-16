package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SourceKeyGeneratorTest {

    private final SourceKeyGenerator generator = new SourceKeyGenerator();

    @Test
    void createsStableDuplicateKeyFromSourceFields() {
        var item = new PublicDataApartmentItem(
                "11680",
                "역삼동",
                "12-3",
                "예시아파트",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                1250000000L,
                null,
                null,
                "1"
        );

        var first = generator.generate(item, TransactionType.SALE);
        var second = generator.generate(item, TransactionType.SALE);

        assertThat(first).isEqualTo(second);
        assertThat(first).isEqualTo("PUBLIC_DATA|APT|SALE|11680|2026-05-20|역삼동|12-3|예시아파트|84.95|15|1");
    }

    @Test
    void includesTransactionTypeInSourceKeyForIdempotentUpserts() {
        var item = new PublicDataApartmentItem(
                "11680",
                "역삼동",
                "12-3",
                "예시아파트",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                null,
                600000000L,
                0L,
                "1"
        );

        var jeonse = generator.generate(item, TransactionType.JEONSE);
        var monthlyRent = generator.generate(item, TransactionType.MONTHLY_RENT);

        assertThat(jeonse).isNotEqualTo(monthlyRent);
        assertThat(jeonse).contains("|JEONSE|");
        assertThat(monthlyRent).contains("|MONTHLY_RENT|");
    }
}
