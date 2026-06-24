package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jiber.backend.property.dto.PropertyType;
import com.jiber.backend.property.dto.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicDataImportServiceTest {

    private final LawdCodeRegistry lawdCodeRegistry = mock(LawdCodeRegistry.class);
    private final PublicDataApiClient publicDataApiClient = mock(PublicDataApiClient.class);
    private final PublicDataTransactionMapper transactionMapper = mock(PublicDataTransactionMapper.class);
    private final AddressNormalizer addressNormalizer = mock(AddressNormalizer.class);
    private final KakaoGeocodingClient kakaoGeocodingClient = mock(KakaoGeocodingClient.class);
    private final PublicDataImportMapper importMapper = mock(PublicDataImportMapper.class);
    private final CanonicalApartmentUpsertService canonicalUpsertService = mock(CanonicalApartmentUpsertService.class);

    @Test
    void dryRunReturnsSummaryWithoutExternalApiOrDbCallsEvenWhenKeysAreConfigured() {
        var service = service(properties("public-data-key", "kakao-key"));

        var summary = service.importRecentApartmentTransactions(new PublicDataImportCommand(true, 1));

        assertThat(summary).isEqualTo(PublicDataImportSummary.empty(true));
        verifyNoInteractions(
                lawdCodeRegistry,
                publicDataApiClient,
                transactionMapper,
                addressNormalizer,
                kakaoGeocodingClient,
                importMapper,
                canonicalUpsertService
        );
    }

    @Test
    void liveModeFailsFastWhenPublicDataServiceKeyIsMissing() {
        var service = service(properties("", "kakao-key"));

        assertThatThrownBy(() -> service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 1)))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("PUBLIC_DATA_SERVICE_KEY")
                .hasMessageNotContaining("kakao-key");
        verifyNoInteractions(lawdCodeRegistry, publicDataApiClient, kakaoGeocodingClient, importMapper);
    }

    @Test
    void liveModeFailsFastWhenKakaoRestApiKeyIsMissing() {
        var service = service(properties("public-data-key", ""));

        assertThatThrownBy(() -> service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 1)))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("KAKAO_REST_API_KEY")
                .hasMessageNotContaining("public-data-key");
        verifyNoInteractions(lawdCodeRegistry, publicDataApiClient, kakaoGeocodingClient, importMapper);
    }

    @Test
    void liveModeStopsWhenLimitIsReached() {
        var service = service(properties("public-data-key", "kakao-key"));
        var lawdCode = new LawdCode(PublicDataTargetRegion.SEOUL, "11680", "서울특별시", "강남구");
        var firstItem = item("12-3", "1");
        var secondItem = item("12-4", "2");
        var address = new NormalizedAddress(
                "서울특별시",
                "강남구",
                "역삼동",
                "12-3",
                "서울특별시 강남구 역삼동 12-3",
                "서울특별시|강남구|역삼동|12-3"
        );
        var transaction = new ImportedApartmentTransaction(
                "source-1",
                PropertyType.APARTMENT,
                TransactionType.SALE,
                "11680",
                "역삼동",
                "12-3",
                "예시아파트",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                1_250_000_000L,
                null,
                0L
        );
        when(lawdCodeRegistry.findByRegions(List.of(PublicDataTargetRegion.SEOUL))).thenReturn(List.of(lawdCode));
        when(publicDataApiClient.fetchApartmentPage(eq(PublicDataApiType.SALE), eq("11680"), any(YearMonth.class), eq(1), eq(100)))
                .thenReturn(new PublicDataApartmentPage(1, 100, 2, List.of(firstItem, secondItem)));
        when(transactionMapper.toImportedTransaction(firstItem, PublicDataApiType.SALE)).thenReturn(transaction);
        when(addressNormalizer.normalize("서울특별시", "강남구", "역삼동", "12-3")).thenReturn(address);
        when(importMapper.findGeocodingByAddressKey(address.addressKey())).thenReturn(Optional.empty());
        when(kakaoGeocodingClient.geocode(address)).thenReturn(GeocodingResult.success(
                address.fullAddress(),
                new BigDecimal("37.5001000"),
                new BigDecimal("127.0364000")
        ));

        var summary = service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 1));

        assertThat(summary.dryRun()).isFalse();
        assertThat(summary.fetchedCount()).isEqualTo(2);
        assertThat(summary.stagedCount()).isEqualTo(1);
        assertThat(summary.geocodedCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isZero();
        verify(publicDataApiClient, times(1)).fetchApartmentPage(eq(PublicDataApiType.SALE), eq("11680"), any(YearMonth.class), eq(1), eq(100));
        verify(publicDataApiClient, never()).fetchApartmentPage(eq(PublicDataApiType.RENT), anyString(), any(YearMonth.class), anyInt(), anyInt());
        verify(importMapper, times(1)).upsertRawTransaction(any(PublicDataRawTransactionRecord.class));
        verify(kakaoGeocodingClient, times(1)).geocode(address);
        verify(canonicalUpsertService, times(1)).upsertEligibleRawRows(any());
        verify(importMapper, times(1)).markImportRunSucceeded(any(PublicDataImportRunRecord.class));
    }

    @Test
    void liveModeReusesGeocodingResultForDuplicateAddressInSameRun() {
        var service = service(properties("public-data-key", "kakao-key"));
        var lawdCode = new LawdCode(PublicDataTargetRegion.SEOUL, "11680", "Seoul", "Gangnam");
        var firstItem = item("12-3", "1");
        var secondItem = item("12-3", "2");
        var address = new NormalizedAddress(
                "Seoul",
                "Gangnam",
                "Yeoksam",
                "12-3",
                "Seoul Gangnam Yeoksam 12-3",
                "Seoul Gangnam Yeoksam 12-3"
        );
        var firstTransaction = transaction("source-1", "12-3");
        var secondTransaction = transaction("source-2", "12-3");
        when(lawdCodeRegistry.findByRegions(List.of(PublicDataTargetRegion.SEOUL))).thenReturn(List.of(lawdCode));
        when(publicDataApiClient.fetchApartmentPage(eq(PublicDataApiType.SALE), eq("11680"), any(YearMonth.class), eq(1), eq(100)))
                .thenReturn(new PublicDataApartmentPage(1, 100, 2, List.of(firstItem, secondItem)));
        when(transactionMapper.toImportedTransaction(firstItem, PublicDataApiType.SALE)).thenReturn(firstTransaction);
        when(transactionMapper.toImportedTransaction(secondItem, PublicDataApiType.SALE)).thenReturn(secondTransaction);
        when(addressNormalizer.normalize(eq("Seoul"), eq("Gangnam"), anyString(), eq("12-3"))).thenReturn(address);
        when(importMapper.findGeocodingByAddressKey(address.addressKey())).thenReturn(Optional.empty());
        when(kakaoGeocodingClient.geocode(address)).thenReturn(GeocodingResult.success(
                address.fullAddress(),
                new BigDecimal("37.5001000"),
                new BigDecimal("127.0364000")
        ));

        var summary = service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 2));

        assertThat(summary.stagedCount()).isEqualTo(2);
        assertThat(summary.geocodedCount()).isEqualTo(2);
        var rawRecordCaptor = ArgumentCaptor.forClass(PublicDataRawTransactionRecord.class);
        verify(importMapper, times(2)).upsertRawTransaction(rawRecordCaptor.capture());
        assertThat(rawRecordCaptor.getAllValues())
                .extracting(PublicDataRawTransactionRecord::geocodingStatus)
                .containsOnly(GeocodingStatus.SUCCESS.name());
        verify(importMapper, times(1)).findGeocodingByAddressKey(address.addressKey());
        verify(kakaoGeocodingClient, times(1)).geocode(address);
        verify(importMapper, never()).updateRawGeocodingStatus(anyString(), any());
    }

    @Test
    void liveModeUsesOnlyConfiguredApiTypes() {
        var service = service(properties("public-data-key", "kakao-key", List.of(PublicDataApiType.RENT)));
        var lawdCode = new LawdCode(PublicDataTargetRegion.SEOUL, "11680", "Seoul", "Gangnam");
        var rentItem = item("12-3", "rent-1");
        var address = new NormalizedAddress(
                "Seoul",
                "Gangnam",
                "Yeoksam",
                "12-3",
                "Seoul Gangnam Yeoksam 12-3",
                "Seoul Gangnam Yeoksam 12-3"
        );
        var transaction = new ImportedApartmentTransaction(
                "rent-source-1",
                PropertyType.APARTMENT,
                TransactionType.JEONSE,
                "11680",
                "Yeoksam",
                "12-3",
                "Example Apartment",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                null,
                300_000_000L,
                0L
        );
        when(lawdCodeRegistry.findByRegions(List.of(PublicDataTargetRegion.SEOUL))).thenReturn(List.of(lawdCode));
        when(publicDataApiClient.fetchApartmentPage(eq(PublicDataApiType.RENT), eq("11680"), any(YearMonth.class), eq(1), eq(100)))
                .thenReturn(new PublicDataApartmentPage(1, 100, 1, List.of(rentItem)));
        when(transactionMapper.toImportedTransaction(rentItem, PublicDataApiType.RENT)).thenReturn(transaction);
        when(addressNormalizer.normalize(eq("Seoul"), eq("Gangnam"), anyString(), eq("12-3"))).thenReturn(address);
        when(importMapper.findGeocodingByAddressKey(address.addressKey())).thenReturn(Optional.empty());
        when(kakaoGeocodingClient.geocode(address)).thenReturn(GeocodingResult.success(
                address.fullAddress(),
                new BigDecimal("37.5001000"),
                new BigDecimal("127.0364000")
        ));

        var summary = service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 1));

        assertThat(summary.stagedCount()).isEqualTo(1);
        verify(publicDataApiClient, times(1)).fetchApartmentPage(eq(PublicDataApiType.RENT), eq("11680"), any(YearMonth.class), eq(1), eq(100));
        verify(publicDataApiClient, never()).fetchApartmentPage(eq(PublicDataApiType.SALE), anyString(), any(YearMonth.class), anyInt(), anyInt());
    }

    @Test
    void liveModeStoresNonApartmentPropertyTypeOnRawTransactions() {
        var service = service(properties("public-data-key", "kakao-key", List.of(PublicDataApiType.OFFICETEL_RENT)));
        var lawdCode = new LawdCode(PublicDataTargetRegion.SEOUL, "11680", "Seoul", "Gangnam");
        var rentItem = item("33-4", "officetel-rent-1");
        var address = new NormalizedAddress(
                "Seoul",
                "Gangnam",
                "Yeoksam",
                "33-4",
                "Seoul Gangnam Yeoksam 33-4",
                "Seoul Gangnam Yeoksam 33-4"
        );
        var transaction = new ImportedApartmentTransaction(
                "officetel-rent-source-1",
                PropertyType.OFFICETEL,
                TransactionType.JEONSE,
                "11680",
                "Yeoksam",
                "33-4",
                "Example Officetel",
                new BigDecimal("29.70"),
                8,
                2020,
                LocalDate.of(2026, 6, 12),
                null,
                300_000_000L,
                0L
        );
        when(lawdCodeRegistry.findByRegions(List.of(PublicDataTargetRegion.SEOUL))).thenReturn(List.of(lawdCode));
        when(publicDataApiClient.fetchApartmentPage(eq(PublicDataApiType.OFFICETEL_RENT), eq("11680"), any(YearMonth.class), eq(1), eq(100)))
                .thenReturn(new PublicDataApartmentPage(1, 100, 1, List.of(rentItem)));
        when(transactionMapper.toImportedTransaction(rentItem, PublicDataApiType.OFFICETEL_RENT)).thenReturn(transaction);
        when(addressNormalizer.normalize(eq("Seoul"), eq("Gangnam"), anyString(), eq("33-4"))).thenReturn(address);
        when(importMapper.findGeocodingByAddressKey(address.addressKey())).thenReturn(Optional.empty());
        when(kakaoGeocodingClient.geocode(address)).thenReturn(GeocodingResult.success(
                address.fullAddress(),
                new BigDecimal("37.5001000"),
                new BigDecimal("127.0364000")
        ));

        service.importRecentApartmentTransactions(new PublicDataImportCommand(false, 1));

        var rawRecordCaptor = ArgumentCaptor.forClass(PublicDataRawTransactionRecord.class);
        verify(importMapper).upsertRawTransaction(rawRecordCaptor.capture());
        assertThat(rawRecordCaptor.getValue().propertyType()).isEqualTo(PropertyType.OFFICETEL);
    }

    private PublicDataImportService service(PublicDataImportProperties properties) {
        return new PublicDataImportService(
                properties,
                lawdCodeRegistry,
                publicDataApiClient,
                transactionMapper,
                addressNormalizer,
                kakaoGeocodingClient,
                importMapper,
                canonicalUpsertService
        );
    }

    private PublicDataImportProperties properties(String serviceKey, String kakaoKey) {
        return properties(serviceKey, kakaoKey, List.of(PublicDataApiType.SALE, PublicDataApiType.RENT));
    }

    private PublicDataImportProperties properties(String serviceKey, String kakaoKey, List<PublicDataApiType> apiTypes) {
        return new PublicDataImportProperties(
                serviceKey,
                1,
                List.of(PublicDataTargetRegion.SEOUL),
                apiTypes,
                true,
                false,
                100,
                100,
                "https://apis.data.go.kr",
                new PublicDataImportProperties.Kakao(kakaoKey, "https://dapi.kakao.com")
        );
    }

    private PublicDataApartmentItem item(String jibun, String sequence) {
        return new PublicDataApartmentItem(
                "11680",
                "역삼동",
                jibun,
                "예시아파트",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                1_250_000_000L,
                null,
                null,
                sequence
        );
    }

    private ImportedApartmentTransaction transaction(String sourceKey, String jibun) {
        return new ImportedApartmentTransaction(
                sourceKey,
                PropertyType.APARTMENT,
                TransactionType.SALE,
                "11680",
                "Yeoksam",
                jibun,
                "Example Apartment",
                new BigDecimal("84.95"),
                15,
                2010,
                LocalDate.of(2026, 5, 20),
                1_250_000_000L,
                null,
                0L
        );
    }
}
