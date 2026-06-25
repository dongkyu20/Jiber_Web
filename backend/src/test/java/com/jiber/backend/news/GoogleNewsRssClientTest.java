package com.jiber.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.news.client.GoogleNewsRssClient;
import com.jiber.backend.news.client.GoogleNewsRssClientException;
import com.jiber.backend.news.config.GoogleNewsRssProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleNewsRssClientTest {

    private final GoogleNewsRssClient client = new GoogleNewsRssClient(
            RestClient.builder(),
            new GoogleNewsRssProperties("https://news.google.com/rss/search", "ko", "KR", "KR:ko")
    );

    @Test
    void parseReadsGoogleNewsRssItems() {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>서울 부동산 거래 &amp; 시장</title>
                      <link>https://news.google.com/rss/articles/example</link>
                      <description><![CDATA[부동산 시장 기사 요약입니다.]]></description>
                      <pubDate>Tue, 25 Jun 2026 08:30:00 +0900</pubDate>
                      <source url="https://example.com">Example News</source>
                    </item>
                  </channel>
                </rss>
                """;

        var items = client.parse(xml);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("서울 부동산 거래 & 시장");
        assertThat(items.get(0).link()).isEqualTo("https://news.google.com/rss/articles/example");
        assertThat(items.get(0).description()).isEqualTo("부동산 시장 기사 요약입니다.");
        assertThat(items.get(0).source()).isEqualTo("Example News");
    }

    @Test
    void parseRejectsMalformedXml() {
        assertThatThrownBy(() -> client.parse("<rss><channel><item></channel></rss>"))
                .isInstanceOf(GoogleNewsRssClientException.class);
    }
}
