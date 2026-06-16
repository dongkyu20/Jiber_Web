package com.jiber.backend.publicdata;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "jiber.public-data")
public record PublicDataImportProperties(
        @DefaultValue("") String serviceKey,
        @DefaultValue("12") int importMonths,
        @DefaultValue("SEOUL,BUSAN") List<PublicDataTargetRegion> targetRegions,
        @DefaultValue("false") boolean enabled,
        @DefaultValue("true") boolean dryRun,
        @DefaultValue("100") int limit,
        @DefaultValue("100") int pageSize,
        @DefaultValue("https://apis.data.go.kr") String baseUrl,
        @DefaultValue Kakao kakao
) {
    public record Kakao(
            @DefaultValue("") String restApiKey,
        @DefaultValue("https://dapi.kakao.com") String baseUrl
    ) {
    }

    public void validateLiveImportSecrets() {
        if (!StringUtils.hasText(serviceKey)) {
            throw new PublicDataClientException("PUBLIC_DATA_SERVICE_KEY is required for live public data import.");
        }
        if (kakao == null || !StringUtils.hasText(kakao.restApiKey())) {
            throw new PublicDataClientException("KAKAO_REST_API_KEY is required for live public data import.");
        }
    }
}
