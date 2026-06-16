package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KakaoGeocodingResponseParserTest {

    private final KakaoGeocodingResponseParser parser = new KakaoGeocodingResponseParser();

    @Test
    void parsesFirstAddressDocumentCoordinates() {
        var result = parser.parse("서울특별시 강남구 역삼동 12-3", FixtureText.read("fixtures/kakao/address-success.json"));

        assertThat(result.status()).isEqualTo(GeocodingStatus.SUCCESS);
        assertThat(result.latitude()).isEqualByComparingTo("37.5001000");
        assertThat(result.longitude()).isEqualByComparingTo("127.0364000");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void returnsZeroResultWhenKakaoHasNoDocuments() {
        var result = parser.parse("서울특별시 강남구 역삼동 0", """
                {"documents":[]}
                """);

        assertThat(result.status()).isEqualTo(GeocodingStatus.ZERO_RESULT);
        assertThat(result.failureReason()).isEqualTo("KAKAO_ZERO_RESULT");
    }
}
