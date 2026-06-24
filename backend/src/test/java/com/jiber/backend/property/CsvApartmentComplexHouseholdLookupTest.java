package com.jiber.backend.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvApartmentComplexHouseholdLookupTest {

    @Test
    void findsHouseholdCountByNormalizedRegionLegalDongAndComplexName() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "서울특별시",
                        "종로구",
                        "무악동",
                        "경희궁 롯데캐슬",
                        "서울특별시 종로구 무악동 89 경희궁롯데캐슬",
                        "서울특별시 종로구 통일로 230",
                        195
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("서울특별시");
        row.setSigungu("종로구");
        row.setLegalDong("무악동");
        row.setName("경희궁롯데캐슬");
        row.setJibunAddress("서울특별시 종로구 무악동 89");

        assertThat(lookup.findHouseholdCount(row)).contains(195);
    }

    @Test
    void findsHouseholdCountByScoredSubstringNameMatchLikeValuationRepository() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "부산광역시",
                        "남구",
                        "대연동",
                        "대연롯데캐슬레전드1단지",
                        "부산광역시 남구 대연동 1872 대연롯데캐슬레전드1단지",
                        "부산광역시 남구 수영로 135",
                        909
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("부산광역시");
        row.setSigungu("남구");
        row.setLegalDong("대연동");
        row.setName("대연롯데캐슬레전드");

        assertThat(lookup.findHouseholdCount(row)).contains(909);
    }

    @Test
    void findsHouseholdCountFromTransactionApartmentNameHintWhenDisplayNameDoesNotMatch() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "서울특별시",
                        "종로구",
                        "무악동",
                        "경희궁 롯데캐슬아파트",
                        "서울특별시 종로구 무악동 89 경희궁 롯데캐슬아파트",
                        "서울특별시 종로구 통일로 230",
                        195
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("서울특별시");
        row.setSigungu("종로구");
        row.setLegalDong("무악동");
        row.setName("표시명");

        assertThat(lookup.findHouseholdCount(row, List.of("경희궁롯데캐슬"))).contains(195);
    }

    @Test
    void findsSplitBusanComplexFromGeneratedSeoulBusanCsv() {
        var lookup = new CsvApartmentComplexHouseholdLookup();
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("부산광역시");
        row.setSigungu("남구");
        row.setLegalDong("대연동");
        row.setName("대연롯데캐슬레전드1단지");

        assertThat(lookup.findHouseholdCount(row)).contains(3149);
    }

    @Test
    void doesNotUseAmbiguousRegionLegalDongNameWhenHouseholdCountsConflict() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "서울특별시",
                        "종로구",
                        "무악동",
                        "같은단지",
                        "서울특별시 종로구 무악동 1 같은단지",
                        null,
                        100
                ),
                new CsvApartmentComplexHouseholdLookup.Record(
                        "서울특별시",
                        "종로구",
                        "무악동",
                        "같은단지",
                        "서울특별시 종로구 무악동 2 같은단지",
                        null,
                        200
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("서울특별시");
        row.setSigungu("종로구");
        row.setLegalDong("무악동");
        row.setName("같은단지");

        assertThat(lookup.findHouseholdCount(row)).isEmpty();
    }
}
