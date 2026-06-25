package com.jiber.backend.publicdata.client;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KakaoGeocodingResponseParser {

    private final ObjectMapper objectMapper;

    public KakaoGeocodingResponseParser() {
        this(new ObjectMapper());
    }

    static KakaoGeocodingResponseParser forTesting(ObjectMapper objectMapper) {
        return new KakaoGeocodingResponseParser(objectMapper);
    }

    private KakaoGeocodingResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GeocodingResult parse(String query, String json) {
        try {
            var root = objectMapper.readTree(json);
            var documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                return GeocodingResult.failure(query, GeocodingStatus.ZERO_RESULT, "KAKAO_ZERO_RESULT");
            }
            var document = documents.get(0);
            var x = firstText(document.path("address").path("x"), document.path("x"));
            var y = firstText(document.path("address").path("y"), document.path("y"));
            if (x == null || y == null) {
                return GeocodingResult.failure(query, GeocodingStatus.ERROR, "KAKAO_COORDINATE_MISSING");
            }
            return GeocodingResult.success(query, new BigDecimal(y), new BigDecimal(x));
        } catch (Exception exception) {
            return GeocodingResult.failure(query, GeocodingStatus.ERROR, "KAKAO_PARSE_ERROR");
        }
    }

    public List<AddressSearchCandidate> parseCandidates(String query, String json) {
        try {
            var root = objectMapper.readTree(json);
            var documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                return List.of();
            }

            var candidates = new ArrayList<AddressSearchCandidate>();
            documents.forEach(document -> {
                var candidate = toCandidate(document);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            });
            return candidates;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private AddressSearchCandidate toCandidate(JsonNode document) {
        var address = document.path("address");
        var roadAddress = document.path("road_address");
        var roadAddressName = firstText(roadAddress.path("address_name"));
        var jibunAddressName = firstText(address.path("address_name"), document.path("address_name"));
        var fullAddress = firstNonBlank(roadAddressName, jibunAddressName, firstText(document.path("address_name")));
        var x = firstText(address.path("x"), roadAddress.path("x"), document.path("x"));
        var y = firstText(address.path("y"), roadAddress.path("y"), document.path("y"));
        var sido = firstText(address.path("region_1depth_name"), roadAddress.path("region_1depth_name"));
        var sigungu = firstText(address.path("region_2depth_name"), roadAddress.path("region_2depth_name"));
        var legalDong = firstText(address.path("region_3depth_name"), roadAddress.path("region_3depth_name"));

        if (fullAddress == null || x == null || y == null || sido == null || sigungu == null || legalDong == null) {
            return null;
        }

        return new AddressSearchCandidate(
                fullAddress,
                roadAddressName,
                jibunAddressName,
                sido,
                sigungu,
                legalDong,
                new BigDecimal(y),
                new BigDecimal(x)
        );
    }

    private String firstText(JsonNode... nodes) {
        for (var node : nodes) {
            if (node != null && node.isTextual() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
