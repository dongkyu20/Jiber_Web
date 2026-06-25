package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

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

    @Test
    void parsesAddressSearchCandidatesForUserSelection() {
        var results = parser.parseCandidates("래미안 삼성", """
                {
                  "documents": [
                    {
                      "address_name": "서울특별시 강남구 삼성동 123",
                      "x": "127.0567000",
                      "y": "37.5123000",
                      "address": {
                        "address_name": "서울특별시 강남구 삼성동 123",
                        "region_1depth_name": "서울특별시",
                        "region_2depth_name": "강남구",
                        "region_3depth_name": "삼성동",
                        "x": "127.0567000",
                        "y": "37.5123000"
                      },
                      "road_address": {
                        "address_name": "서울특별시 강남구 테헤란로 123"
                      }
                    },
                    {
                      "address_name": "서울특별시 강남구 삼성동 124",
                      "x": "127.0578000",
                      "y": "37.5134000",
                      "address": {
                        "address_name": "서울특별시 강남구 삼성동 124",
                        "region_1depth_name": "서울특별시",
                        "region_2depth_name": "강남구",
                        "region_3depth_name": "삼성동"
                      }
                    }
                  ]
                }
                """);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).fullAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(results.get(0).jibunAddress()).isEqualTo("서울특별시 강남구 삼성동 123");
        assertThat(results.get(0).sido()).isEqualTo("서울특별시");
        assertThat(results.get(0).sigungu()).isEqualTo("강남구");
        assertThat(results.get(0).legalDong()).isEqualTo("삼성동");
        assertThat(results.get(0).latitude()).isEqualByComparingTo("37.5123000");
        assertThat(results.get(0).longitude()).isEqualByComparingTo("127.0567000");
        assertThat(results.get(1).fullAddress()).isEqualTo("서울특별시 강남구 삼성동 124");
    }
}
