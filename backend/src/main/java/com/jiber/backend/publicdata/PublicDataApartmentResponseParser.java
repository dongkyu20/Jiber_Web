package com.jiber.backend.publicdata;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Component
public class PublicDataApartmentResponseParser {

    public PublicDataApartmentPage parse(String xml, PublicDataApiType apiType) {
        try {
            var documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var documentBuilder = documentBuilderFactory.newDocumentBuilder();
            var document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            var body = firstElement(document.getDocumentElement(), "body");
            var pageNo = parseInt(text(body, "pageNo"), 1);
            var numOfRows = parseInt(text(body, "numOfRows"), 10);
            var totalCount = parseInt(text(body, "totalCount"), 0);
            var itemNodes = document.getElementsByTagName("item");
            var items = new ArrayList<PublicDataApartmentItem>();
            for (var index = 0; index < itemNodes.getLength(); index++) {
                var item = (Element) itemNodes.item(index);
                items.add(parseItem(item, apiType, index + 1));
            }
            return new PublicDataApartmentPage(pageNo, numOfRows, totalCount, items);
        } catch (Exception exception) {
            throw new PublicDataParseException("공공데이터 응답을 파싱할 수 없습니다.", exception);
        }
    }

    private PublicDataApartmentItem parseItem(Element item, PublicDataApiType apiType, int sequence) {
        var dealYear = parseInt(text(item, "dealYear"), null);
        var dealMonth = parseInt(text(item, "dealMonth"), null);
        var dealDay = parseInt(text(item, "dealDay"), null);
        var dealDate = dealYear == null || dealMonth == null || dealDay == null
                ? null
                : LocalDate.of(dealYear, dealMonth, dealDay);
        return new PublicDataApartmentItem(
                firstText(item, "sggCd", "LAWD_CD", "lawdCd"),
                firstText(item, "umdNm", "legalDong"),
                firstText(item, "jibun"),
                firstText(item, "aptNm", "apartmentName"),
                parseDecimal(firstText(item, "excluUseAr", "exclusiveAreaM2")),
                parseInt(firstText(item, "floor"), null),
                parseInt(firstText(item, "buildYear", "builtYear"), null),
                dealDate,
                apiType == PublicDataApiType.SALE ? parseAmountKrw(firstText(item, "dealAmount")) : null,
                apiType == PublicDataApiType.RENT ? parseAmountKrw(firstText(item, "deposit", "depositAmount")) : null,
                apiType == PublicDataApiType.RENT ? parseAmountKrw(firstText(item, "monthlyRent", "monthlyRentAmount")) : null,
                firstNonBlank(firstText(item, "sourceSequence"), Integer.toString(sequence))
        );
    }

    private Element firstElement(Element parent, String name) {
        var nodes = parent.getElementsByTagName(name);
        if (nodes.getLength() == 0) {
            return parent;
        }
        return (Element) nodes.item(0);
    }

    private String firstText(Element element, String... names) {
        for (var name : names) {
            var value = text(element, name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String text(Element element, String name) {
        if (element == null) {
            return null;
        }
        var nodes = element.getElementsByTagName(name);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim().replace(",", ""));
    }

    private Integer parseInt(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim().replace(",", ""));
    }

    private Long parseAmountKrw(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim().replace(",", "")) * 10000L;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
