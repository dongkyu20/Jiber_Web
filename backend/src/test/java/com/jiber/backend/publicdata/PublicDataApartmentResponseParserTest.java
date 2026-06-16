package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.TransactionType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PublicDataApartmentResponseParserTest {

    private final PublicDataApartmentResponseParser parser = new PublicDataApartmentResponseParser();

    @Test
    void parsesApartmentSaleXmlFixture() {
        var page = parser.parse(FixtureText.read("fixtures/publicdata/apartment-sale.xml"), PublicDataApiType.SALE);

        assertThat(page.pageNo()).isEqualTo(1);
        assertThat(page.numOfRows()).isEqualTo(10);
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.lawdCd()).isEqualTo("11680");
            assertThat(item.legalDong()).isEqualTo("역삼동");
            assertThat(item.jibun()).isEqualTo("12-3");
            assertThat(item.apartmentName()).isEqualTo("예시아파트");
            assertThat(item.exclusiveAreaM2()).isEqualByComparingTo("84.95");
            assertThat(item.floor()).isEqualTo(15);
            assertThat(item.builtYear()).isEqualTo(2010);
            assertThat(item.dealDate()).isEqualTo(LocalDate.of(2026, 5, 20));
            assertThat(item.dealAmountKrw()).isEqualTo(1250000000L);
        });
    }

    @Test
    void parsesApartmentRentXmlFixtureAndClassifiesJeonseAndMonthlyRent() {
        var page = parser.parse(FixtureText.read("fixtures/publicdata/apartment-rent.xml"), PublicDataApiType.RENT);
        var mapper = new PublicDataTransactionMapper(new SourceKeyGenerator());

        assertThat(page.items()).hasSize(2);
        var jeonse = mapper.toImportedTransaction(page.items().get(0), PublicDataApiType.RENT);
        var monthlyRent = mapper.toImportedTransaction(page.items().get(1), PublicDataApiType.RENT);

        assertThat(jeonse.transactionType()).isEqualTo(TransactionType.JEONSE);
        assertThat(jeonse.depositAmountKrw()).isEqualTo(600000000L);
        assertThat(jeonse.monthlyRentKrw()).isZero();
        assertThat(monthlyRent.transactionType()).isEqualTo(TransactionType.MONTHLY_RENT);
        assertThat(monthlyRent.depositAmountKrw()).isEqualTo(50000000L);
        assertThat(monthlyRent.monthlyRentKrw()).isEqualTo(1200000L);
    }
}
