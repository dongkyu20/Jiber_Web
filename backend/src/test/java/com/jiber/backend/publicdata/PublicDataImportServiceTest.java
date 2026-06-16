package com.jiber.backend.publicdata;

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

import com.jiber.backend.property.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
        verify(canonicalUpsertService, times(1)).decide(eq(transaction), eq(address), any(GeocodingResult.class));
        verify(importMapper, times(1)).markImportRunSucceeded(any(PublicDataImportRunRecord.class));
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
        return new PublicDataImportProperties(
                serviceKey,
                1,
                List.of(PublicDataTargetRegion.SEOUL),
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
}
