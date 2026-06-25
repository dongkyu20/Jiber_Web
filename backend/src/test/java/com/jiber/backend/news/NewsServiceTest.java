package com.jiber.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.news.client.GoogleNewsRssClient;
import com.jiber.backend.news.client.GoogleNewsRssClientException;
import com.jiber.backend.news.client.GoogleNewsRssItem;
import com.jiber.backend.news.dto.NewsSearchRequest;
import com.jiber.backend.news.service.NewsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsServiceTest {

    private GoogleNewsRssClient googleNewsRssClient;
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        googleNewsRssClient = mock(GoogleNewsRssClient.class);
        newsService = new NewsService(googleNewsRssClient);
    }

    @Test
    void searchUsesDefaultRealEstateKeywordAndReturnsSanitizedLatestFeedItems() {
        when(googleNewsRssClient.search("부동산"))
                .thenReturn(List.of(new GoogleNewsRssItem(
                        "서울 <b>부동산</b> 거래 &quot;회복&quot;",
                        "https://news.google.com/rss/articles/example",
                        "서울 아파트 거래량이 <b>증가</b>했습니다.",
                        "Tue, 25 Jun 2026 08:30:00 +0900",
                        "Example News"
                )));

        var response = newsService.search(new NewsSearchRequest(null, null));

        assertThat(response.available()).isTrue();
        assertThat(response.keyword()).isEqualTo("부동산");
        assertThat(response.message()).isEqualTo("Google 뉴스 RSS 검색 결과입니다.");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("서울 부동산 거래 \"회복\"");
        assertThat(response.items().get(0).summary()).isEqualTo("서울 아파트 거래량이 증가했습니다.");
        assertThat(response.items().get(0).link()).isEqualTo("https://news.google.com/rss/articles/example");
        assertThat(response.items().get(0).source()).isEqualTo("Example News");
        verify(googleNewsRssClient).search("부동산");
    }

    @Test
    void searchAddsRealEstateContextForUserEnteredKeywords() {
        when(googleNewsRssClient.search("재건축 부동산"))
                .thenReturn(List.of());

        var response = newsService.search(new NewsSearchRequest("재건축", 10));

        assertThat(response.available()).isTrue();
        assertThat(response.keyword()).isEqualTo("재건축");
        assertThat(response.items()).isEmpty();
        verify(googleNewsRssClient).search("재건축 부동산");
    }

    @Test
    void searchDoesNotDuplicateRealEstateContextWhenKeywordAlreadyContainsIt() {
        when(googleNewsRssClient.search("서울 부동산"))
                .thenReturn(List.of());

        newsService.search(new NewsSearchRequest("서울 부동산", 10));

        verify(googleNewsRssClient).search("서울 부동산");
    }

    @Test
    void searchLimitsFeedItemsByRequestedDisplaySize() {
        when(googleNewsRssClient.search("부동산"))
                .thenReturn(List.of(
                        new GoogleNewsRssItem("첫 기사", "https://example.com/1", "요약", null, ""),
                        new GoogleNewsRssItem("둘째 기사", "https://example.com/2", "요약", null, "")
                ));

        var response = newsService.search(new NewsSearchRequest("부동산", 1));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("첫 기사");
        assertThat(response.items().get(0).source()).isEqualTo("example.com");
    }

    @Test
    void searchMapsGoogleRssFailuresToNewsSearchUnavailable() {
        when(googleNewsRssClient.search("부동산"))
                .thenThrow(new GoogleNewsRssClientException("RSS failed.", null));

        assertThatThrownBy(() -> newsService.search(new NewsSearchRequest("부동산", 20)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NEWS_SEARCH_UNAVAILABLE);
    }
}
