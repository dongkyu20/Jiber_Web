package com.jiber.backend.publicdata;

import java.time.YearMonth;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PublicDataApiClient {

    private final RestClient restClient;
    private final PublicDataImportProperties properties;
    private final PublicDataApartmentResponseParser parser;

    public PublicDataApiClient(
            RestClient.Builder restClientBuilder,
            PublicDataImportProperties properties,
            PublicDataApartmentResponseParser parser
    ) {
        this.restClient = restClientBuilder.baseUrl(stripTrailingSlash(properties.baseUrl())).build();
        this.properties = properties;
        this.parser = parser;
    }

    public PublicDataApartmentPage fetchApartmentPage(
            PublicDataApiType apiType,
            String lawdCd,
            YearMonth dealMonth,
            int pageNo,
            int numOfRows
    ) {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new PublicDataClientException("PUBLIC_DATA_SERVICE_KEY is required.");
        }
        try {
            var body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(apiType.endpointPath())
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("LAWD_CD", lawdCd)
                            .queryParam("DEAL_YMD", dealMonth.toString().replace("-", ""))
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .build())
                    .retrieve()
                    .body(String.class);
            return parser.parse(body, apiType);
        } catch (PublicDataParseException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new PublicDataClientException(SecretRedactor.redact("Public data API request failed.", properties.serviceKey()), exception);
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://apis.data.go.kr";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
