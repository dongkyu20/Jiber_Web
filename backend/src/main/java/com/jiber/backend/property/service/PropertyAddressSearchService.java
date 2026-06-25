package com.jiber.backend.property.service;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorDetail;
import com.jiber.backend.property.dto.NewApartmentAddressSearchResponse;
import com.jiber.backend.publicdata.client.KakaoGeocodingClient;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PropertyAddressSearchService {

    private final KakaoGeocodingClient kakaoGeocodingClient;

    public PropertyAddressSearchService(KakaoGeocodingClient kakaoGeocodingClient) {
        this.kakaoGeocodingClient = kakaoGeocodingClient;
    }

    public List<NewApartmentAddressSearchResponse> searchNewApartmentAddresses(String query) {
        var normalizedQuery = query == null ? "" : query.trim();
        if (!StringUtils.hasText(normalizedQuery) || normalizedQuery.length() < 2) {
            throw ApiException.validation(List.of(new ErrorDetail("query", "주소 검색어를 두 글자 이상 입력해 주세요.")));
        }

        return kakaoGeocodingClient.searchAddressCandidates(normalizedQuery).stream()
                .map(candidate -> new NewApartmentAddressSearchResponse(
                        candidate.fullAddress(),
                        candidate.roadAddress(),
                        candidate.jibunAddress(),
                        candidate.sido(),
                        candidate.sigungu(),
                        candidate.legalDong(),
                        candidate.latitude(),
                        candidate.longitude()
                ))
                .toList();
    }
}
