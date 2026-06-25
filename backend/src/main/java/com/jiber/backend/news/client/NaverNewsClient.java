package com.jiber.backend.news.client;

import com.jiber.backend.news.config.NaverNewsProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverNewsClient {

    private static final String NEWS_SEARCH_PATH = "/v1/search/news.json";

    private final RestClient restClient;
    private final NaverNewsProperties properties;

    public NaverNewsClient(RestClient.Builder restClientBuilder, NaverNewsProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.effectiveBaseUrl()).build();
        this.properties = properties;
    }

    public NaverNewsApiResponse search(String query, int display, String sort) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(NEWS_SEARCH_PATH)
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", 1)
                            .queryParam("sort", sort)
                            .build())
                    .header("X-Naver-Client-Id", properties.effectiveClientId())
                    .header("X-Naver-Client-Secret", properties.effectiveClientSecret())
                    .retrieve()
                    .body(NaverNewsApiResponse.class);
            if (response == null) {
                throw new NaverNewsClientException("Naver news API returned an empty body.", null);
            }
            return response;
        } catch (RestClientException exception) {
            if (exception instanceof RestClientResponseException responseException) {
                throw new NaverNewsClientException(
                        "Naver news API request failed.",
                        responseException.getStatusCode().value(),
                        responseException.getResponseBodyAsString(),
                        responseException
                );
            }
            throw new NaverNewsClientException("Naver news API request failed.", exception);
        }
    }
}
