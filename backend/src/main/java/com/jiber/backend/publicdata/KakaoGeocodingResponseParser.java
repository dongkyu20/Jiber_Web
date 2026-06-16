package com.jiber.backend.publicdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

    private String firstText(JsonNode primary, JsonNode fallback) {
        if (primary != null && primary.isTextual() && !primary.asText().isBlank()) {
            return primary.asText();
        }
        if (fallback != null && fallback.isTextual() && !fallback.asText().isBlank()) {
            return fallback.asText();
        }
        return null;
    }
}
