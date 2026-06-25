package com.jiber.backend.publicdata.client;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoGeocodingClient {

    private final RestClient restClient;
    private final PublicDataImportProperties properties;
    private final KakaoGeocodingResponseParser parser;

    public KakaoGeocodingClient(
            RestClient.Builder restClientBuilder,
            PublicDataImportProperties properties,
            KakaoGeocodingResponseParser parser
    ) {
        this.restClient = restClientBuilder.baseUrl(stripTrailingSlash(properties.kakao().baseUrl())).build();
        this.properties = properties;
        this.parser = parser;
    }

    public GeocodingResult geocode(NormalizedAddress address) {
        if (!StringUtils.hasText(properties.kakao().restApiKey())) {
            return GeocodingResult.failure(address.fullAddress(), GeocodingStatus.ERROR, "KAKAO_REST_API_KEY_MISSING");
        }
        try {
            var body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address.fullAddress())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.kakao().restApiKey())
                    .retrieve()
                    .body(String.class);
            return parser.parse(address.fullAddress(), body);
        } catch (RestClientException exception) {
            return GeocodingResult.failure(
                    address.fullAddress(),
                    GeocodingStatus.ERROR,
                    SecretRedactor.redact("KAKAO_REQUEST_FAILED", properties.kakao().restApiKey())
            );
        }
    }

    public List<AddressSearchCandidate> searchAddressCandidates(String query) {
        var normalizedQuery = query == null ? "" : query.trim();
        if (!StringUtils.hasText(properties.kakao().restApiKey()) || !StringUtils.hasText(normalizedQuery)) {
            return List.of();
        }
        try {
            var body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", normalizedQuery)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.kakao().restApiKey())
                    .retrieve()
                    .body(String.class);
            return parser.parseCandidates(normalizedQuery, body);
        } catch (RestClientException exception) {
            return List.of();
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dapi.kakao.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
