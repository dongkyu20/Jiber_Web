package com.jiber.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiber.backend.news.client.NaverNewsApiItem;
import com.jiber.backend.news.client.NaverNewsApiResponse;
import com.jiber.backend.news.client.NaverNewsClient;
import com.jiber.backend.news.client.NaverNewsClientException;
import com.jiber.backend.news.config.NaverNewsProperties;
import com.jiber.backend.news.dto.NewsSearchRequest;
import com.jiber.backend.news.service.NewsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsServiceTest {

    private NaverNewsClient naverNewsClient;
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        naverNewsClient = mock(NaverNewsClient.class);
        newsService = new NewsService(
                naverNewsClient,
                new NaverNewsProperties("client-id", "client-secret", null, null, "https://openapi.naver.com")
        );
    }

    @Test
    void searchUsesDefaultRealEstateKeywordAndReturnsSanitizedLatestFeedItems() {
        when(naverNewsClient.search("부동산", 20, "date"))
                .thenReturn(new NaverNewsApiResponse(
                        "Tue, 25 Jun 2026 09:00:00 +0900",
                        1,
                        1,
                        20,
                        List.of(new NaverNewsApiItem(
                                "서울 <b>부동산</b> 거래 &quot;회복&quot;",
                                "https://news.naver.com/article/001/0000000001",
                                "https://example.com/economy/real-estate",
                                "서울 아파트 거래량이 <b>증가</b>했습니다.",
                                "Tue, 25 Jun 2026 08:30:00 +0900"
                        ))
                ));

        var response = newsService.search(new NewsSearchRequest(null, null));

        assertThat(response.available()).isTrue();
        assertThat(response.keyword()).isEqualTo("부동산");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("서울 부동산 거래 \"회복\"");
        assertThat(response.items().get(0).summary()).isEqualTo("서울 아파트 거래량이 증가했습니다.");
        assertThat(response.items().get(0).naverLink()).isEqualTo("https://news.naver.com/article/001/0000000001");
        assertThat(response.items().get(0).source()).isEqualTo("example.com");
        verify(naverNewsClient).search("부동산", 20, "date");
    }

    @Test
    void searchDoesNotCallNaverWhenCredentialsAreMissing() {
        var serviceWithoutCredentials = new NewsService(
                naverNewsClient,
                new NaverNewsProperties("", "", null, null, "https://openapi.naver.com")
        );

        var response = serviceWithoutCredentials.search(new NewsSearchRequest("아파트", 10));

        assertThat(response.available()).isFalse();
        assertThat(response.keyword()).isEqualTo("아파트");
        assertThat(response.items()).isEmpty();
        assertThat(response.message()).contains("네이버 뉴스 검색 API 키");
        verify(naverNewsClient, never()).search("아파트", 10, "date");
    }

    @Test
    void searchReturnsUnavailableMessageWhenNaverRejectsCredentials() {
        when(naverNewsClient.search("부동산", 20, "date"))
                .thenThrow(new NaverNewsClientException(
                        "Naver rejected credentials.",
                        401,
                        "{\"errorMessage\":\"Scopes are Empty : Authentication failed.\",\"errorCode\":\"024\"}",
                        null
                ));

        var response = newsService.search(new NewsSearchRequest("부동산", 20));

        assertThat(response.available()).isFalse();
        assertThat(response.items()).isEmpty();
        assertThat(response.message()).contains("네이버 개발자센터");
        assertThat(response.message()).contains("검색 API");
    }
}
