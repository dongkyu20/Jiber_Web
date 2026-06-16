package com.jiber.backend.publicdata;

import org.springframework.stereotype.Service;

@Service
public class CanonicalApartmentUpsertService {

    public CanonicalApartmentUpsertDecision decide(
            ImportedApartmentTransaction transaction,
            NormalizedAddress address,
            GeocodingResult geocoding
    ) {
        if (geocoding.status() != GeocodingStatus.SUCCESS) {
            return new CanonicalApartmentUpsertDecision(false, "좌표가 없는 거래는 지도 노출 canonical 테이블에 반영하지 않습니다.");
        }
        return new CanonicalApartmentUpsertDecision(false, "Phase 1은 raw staging과 geocoding cache 저장까지 구현하고 canonical upsert는 분리합니다.");
    }
}
