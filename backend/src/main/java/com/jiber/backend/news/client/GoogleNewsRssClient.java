package com.jiber.backend.news.client;

import com.jiber.backend.news.config.GoogleNewsRssProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Component
public class GoogleNewsRssClient {

    private final RestClient restClient;
    private final GoogleNewsRssProperties properties;

    public GoogleNewsRssClient(RestClient.Builder restClientBuilder, GoogleNewsRssProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.effectiveBaseUrl()).build();
        this.properties = properties;
    }

    public List<GoogleNewsRssItem> search(String query) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", query)
                            .queryParam("hl", properties.effectiveHl())
                            .queryParam("gl", properties.effectiveGl())
                            .queryParam("ceid", properties.effectiveCeid())
                            .build())
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                throw new GoogleNewsRssClientException("Google news RSS returned an empty body.", null);
            }
            return parse(response);
        } catch (RestClientException exception) {
            throw new GoogleNewsRssClientException("Google news RSS request failed.", exception);
        }
    }

    public List<GoogleNewsRssItem> parse(String xml) {
        try {
            var documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setExpandEntityReferences(false);

            var documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new ThrowingErrorHandler());
            var document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            var itemNodes = document.getElementsByTagName("item");
            var items = new ArrayList<GoogleNewsRssItem>();

            for (var index = 0; index < itemNodes.getLength(); index++) {
                var node = itemNodes.item(index);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                var element = (Element) node;
                items.add(new GoogleNewsRssItem(
                        childText(element, "title"),
                        childText(element, "link"),
                        childText(element, "description"),
                        childText(element, "pubDate"),
                        childText(element, "source")
                ));
            }
            return items;
        } catch (SAXException exception) {
            throw new GoogleNewsRssClientException("Google news RSS XML parsing failed.", exception);
        } catch (Exception exception) {
            throw new GoogleNewsRssClientException("Google news RSS parsing failed.", exception);
        }
    }

    private String childText(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        var node = nodes.item(0);
        return node == null || node.getTextContent() == null ? "" : node.getTextContent();
    }

    private static class ThrowingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
