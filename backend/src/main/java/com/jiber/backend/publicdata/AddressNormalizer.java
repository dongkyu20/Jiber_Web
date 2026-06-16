package com.jiber.backend.publicdata;

import org.springframework.stereotype.Component;

@Component
public class AddressNormalizer {

    public NormalizedAddress normalize(String sido, String sigungu, String legalDong, String jibun) {
        var normalizedSido = normalizePart(sido);
        var normalizedSigungu = normalizePart(sigungu);
        var normalizedLegalDong = normalizePart(legalDong);
        var normalizedJibun = normalizePart(jibun);
        var fullAddress = String.join(" ", normalizedSido, normalizedSigungu, normalizedLegalDong, normalizedJibun);
        var addressKey = String.join("|", normalizedSido, normalizedSigungu, normalizedLegalDong, normalizedJibun);
        return new NormalizedAddress(
                normalizedSido,
                normalizedSigungu,
                normalizedLegalDong,
                normalizedJibun,
                fullAddress,
                addressKey
        );
    }

    private String normalizePart(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
