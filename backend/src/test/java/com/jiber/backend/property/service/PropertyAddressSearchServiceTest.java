package com.jiber.backend.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.publicdata.client.KakaoGeocodingClient;
import com.jiber.backend.publicdata.dto.AddressSearchCandidate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyAddressSearchServiceTest {

    private final KakaoGeocodingClient kakaoGeocodingClient = mock(KakaoGeocodingClient.class);
    private final PropertyAddressSearchService service = new PropertyAddressSearchService(kakaoGeocodingClient);

    @Test
    void mapsKakaoAddressCandidatesForNewApartmentAnalysis() {
        when(kakaoGeocodingClient.searchAddressCandidates("래미안 삼성")).thenReturn(List.of(
                new AddressSearchCandidate(
                        "서울특별시 강남구 테헤란로 123",
                        "서울특별시 강남구 테헤란로 123",
                        "서울특별시 강남구 삼성동 123",
                        "서울특별시",
                        "강남구",
                        "삼성동",
                        new BigDecimal("37.5123000"),
                        new BigDecimal("127.0567000")
                )
        ));

        var results = service.searchNewApartmentAddresses(" 래미안 삼성 ");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).fullAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(results.get(0).sido()).isEqualTo("서울특별시");
        assertThat(results.get(0).sigungu()).isEqualTo("강남구");
        assertThat(results.get(0).legalDong()).isEqualTo("삼성동");
        assertThat(results.get(0).latitude()).isEqualByComparingTo("37.5123000");
        assertThat(results.get(0).longitude()).isEqualByComparingTo("127.0567000");
    }

    @Test
    void rejectsTooShortAddressQuery() {
        assertThatThrownBy(() -> service.searchNewApartmentAddresses("  a "))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
