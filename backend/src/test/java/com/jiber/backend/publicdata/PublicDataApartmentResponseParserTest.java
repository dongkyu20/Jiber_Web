package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import com.jiber.backend.property.dto.TransactionType;
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

    @Test
    void parsesOfficetelSaleXmlFixture() {
        var page = parser.parse(FixtureText.read("fixtures/publicdata/officetel-sale.xml"), PublicDataApiType.OFFICETEL_SALE);
        var mapper = new PublicDataTransactionMapper(new SourceKeyGenerator());

        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.lawdCd()).isEqualTo("11680");
            assertThat(item.legalDong()).isEqualTo("Yeoksam");
            assertThat(item.jibun()).isEqualTo("10-1");
            assertThat(item.apartmentName()).isEqualTo("Sample Officetel");
            assertThat(item.exclusiveAreaM2()).isEqualByComparingTo("29.7");
            assertThat(item.floor()).isEqualTo(8);
            assertThat(item.builtYear()).isEqualTo(2020);
            assertThat(item.dealDate()).isEqualTo(LocalDate.of(2026, 6, 12));
            assertThat(item.dealAmountKrw()).isEqualTo(320000000L);
        });

        var transaction = mapper.toImportedTransaction(page.items().get(0), PublicDataApiType.OFFICETEL_SALE);

        assertThat(transaction.propertyType().name()).isEqualTo("OFFICETEL");
        assertThat(transaction.transactionType()).isEqualTo(TransactionType.SALE);
        assertThat(transaction.sourceKey()).contains("OFFICETEL");
    }

    @Test
    void parsesVillaRentXmlFixture() {
        var page = parser.parse(FixtureText.read("fixtures/publicdata/villa-rent.xml"), PublicDataApiType.VILLA_RENT);
        var mapper = new PublicDataTransactionMapper(new SourceKeyGenerator());

        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.apartmentName()).isEqualTo("Sample Villa");
            assertThat(item.depositAmountKrw()).isEqualTo(150000000L);
            assertThat(item.monthlyRentKrw()).isEqualTo(750000L);
        });

        var transaction = mapper.toImportedTransaction(page.items().get(0), PublicDataApiType.VILLA_RENT);

        assertThat(transaction.propertyType().name()).isEqualTo("VILLA");
        assertThat(transaction.transactionType()).isEqualTo(TransactionType.MONTHLY_RENT);
        assertThat(transaction.sourceKey()).contains("VILLA");
    }
}
