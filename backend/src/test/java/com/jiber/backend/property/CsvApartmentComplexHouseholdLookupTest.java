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
    void findsShortComplexNameWhenRegionAndDongAreSpecific() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC601\uB4F1\uD3EC\uAD6C",
                        "\uC5EC\uC758\uB3C4\uB3D9",
                        "\uC5EC\uC758\uB3C4\uC2DC\uBC94\uC544\uD30C\uD2B8",
                        null,
                        null,
                        1584
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC601\uB4F1\uD3EC\uAD6C");
        row.setLegalDong("\uC5EC\uC758\uB3C4\uB3D9");
        row.setName("\uC2DC\uBC94");

        assertThat(lookup.findHouseholdCount(row)).contains(1584);
    }

    @Test
    void findsComplexWithNewTownPrefixAndTransactionSuffixByCoreOverlap() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC740\uD3C9\uAD6C",
                        "\uC9C4\uAD00\uB3D9",
                        "\uC740\uD3C9\uB274\uD0C0\uC6B4\uC0C1\uB9BC\uB9C8\uC7447\uB2E8\uC9C0",
                        null,
                        null,
                        378
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC740\uD3C9\uAD6C");
        row.setLegalDong("\uC9C4\uAD00\uB3D9");
        row.setName("\uC0C1\uB9BC\uB9C8\uC7447\uB2E8\uC9C0\uC544\uC774\uD30C\uD06C(713~718\uB3D9)BL1-11");

        assertThat(lookup.findHouseholdCount(row)).contains(378);
    }

    @Test
    void findsComplexAcrossKoreanEnglishBrandAlias() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC1A1\uD30C\uAD6C",
                        "\uAC70\uC5EC\uB3D9",
                        "\uC774\uD3B8\uD55C\uC138\uC0C1 \uC1A1\uD30C\uD30C\uD06C\uC13C\uD2B8\uB7F4",
                        null,
                        null,
                        1199
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC1A1\uD30C\uAD6C");
        row.setLegalDong("\uAC70\uC5EC\uB3D9");
        row.setName("e\uD3B8\uD55C\uC138\uC0C1\uC1A1\uD30C\uD30C\uD06C\uC13C\uD2B8\uB7F4");

        assertThat(lookup.findHouseholdCount(row)).contains(1199);
    }

    @Test
    void prefersNonRentalRecordWhenWantedNameDoesNotSayRental() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC911\uAD6C",
                        "\uC2E0\uB2F9\uB3D9",
                        "\uC2E0\uB2F9\uC57D\uC218\uD558\uC774\uCE20",
                        null,
                        null,
                        1598
                ),
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC911\uAD6C",
                        "\uC2E0\uB2F9\uB3D9",
                        "\uC57D\uC218\uD558\uC774\uCE20\uC544\uD30C\uD2B8(\uC784\uB300)",
                        null,
                        null,
                        684
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC911\uAD6C");
        row.setLegalDong("\uC2E0\uB2F9\uB3D9");
        row.setName("\uC57D\uC218\uD558\uC774\uCE20");

        assertThat(lookup.findHouseholdCount(row)).contains(1598);
    }

    @Test
    void findsComplexAcrossMokdongNewTownAlias() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC591\uCC9C\uAD6C",
                        "\uC2E0\uC815\uB3D9",
                        "\uBAA9\uB3D911\uB2E8\uC9C0",
                        null,
                        null,
                        1595
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC591\uCC9C\uAD6C");
        row.setLegalDong("\uC2E0\uC815\uB3D9");
        row.setName("\uBAA9\uB3D9\uC2E0\uC2DC\uAC00\uC9C011");

        assertThat(lookup.findHouseholdCount(row)).contains(1595);
    }

    @Test
    void findsComplexAcrossRomanNumeralSuffix() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC1A1\uD30C\uAD6C",
                        "\uAC70\uC5EC\uB3D9",
                        "\uC1A1\uD30C\uB808\uC774\uD06C\uD30C\uD06C\uD638\uBC18\uC368\uBC0B1\uCC28",
                        null,
                        null,
                        689
                ),
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC1A1\uD30C\uAD6C",
                        "\uAC70\uC5EC\uB3D9",
                        "\uC1A1\uD30C\uB808\uC774\uD06C\uD30C\uD06C\uD638\uBC18\uC368\uBC0B2",
                        null,
                        null,
                        700
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC1A1\uD30C\uAD6C");
        row.setLegalDong("\uAC70\uC5EC\uB3D9");
        row.setName("\uC1A1\uD30C\uB808\uC774\uD06C\uD30C\uD06C\uD638\uBC18\uC368\uBC0B\u2160");

        assertThat(lookup.findHouseholdCount(row)).contains(689);
    }

    @Test
    void ignoresParentheticalAliasWhenMatchingDisplayName() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uAC15\uB0A8\uAD6C",
                        "\uC5ED\uC0BC\uB3D9",
                        "\uB798\uBBF8\uC548\uADF8\uB808\uC774\uD2BC",
                        null,
                        null,
                        464
                ),
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uAC15\uB0A8\uAD6C",
                        "\uC5ED\uC0BC\uB3D9",
                        "\uB798\uBBF8\uC548\uADF8\uB808\uC774\uD2BC3\uCC28",
                        null,
                        null,
                        476
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uAC15\uB0A8\uAD6C");
        row.setLegalDong("\uC5ED\uC0BC\uB3D9");
        row.setName("\uB798\uBBF8\uC548\uADF8\uB808\uC774\uD2BC(\uC9C4\uB2EC\uB7982\uCC28)");

        assertThat(lookup.findHouseholdCount(row)).contains(464);
    }

    @Test
    void findsComplexWhenMainNameAndNumberAreSplitAroundParentheses() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uB178\uC6D0\uAD6C",
                        "\uD558\uACC4\uB3D9",
                        "\uD558\uACC4 6\uB2E8\uC9C0 \uC7A5\uBBF8\uC544\uD30C\uD2B8",
                        null,
                        null,
                        1880
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uB178\uC6D0\uAD6C");
        row.setLegalDong("\uD558\uACC4\uB3D9");
        row.setName("\uC7A5\uBBF8(\uC2DC\uC6016)");

        assertThat(lookup.findHouseholdCount(row)).contains(1880);
    }

    @Test
    void findsNewTownBlockRecordAcrossBrandAndBlockNotation() {
        var lookup = new CsvApartmentComplexHouseholdLookup(List.of(
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC740\uD3C9\uAD6C",
                        "\uC9C4\uAD00\uB3D9",
                        "\uC740\uD3C9\uB274\uD0C0\uC6B4\uC81C\uAC01\uB9D05\uB2E8\uC9C0\uC81C1",
                        null,
                        null,
                        330
                ),
                new CsvApartmentComplexHouseholdLookup.Record(
                        "\uC11C\uC6B8\uD2B9\uBCC4\uC2DC",
                        "\uC740\uD3C9\uAD6C",
                        "\uC9C4\uAD00\uB3D9",
                        "\uC740\uD3C9\uB274\uD0C0\uC6B4\uC81C\uAC01\uB9D05\uB2E8\uC9C0\uC81C4",
                        null,
                        null,
                        412
                )
        ));
        var row = new PropertyDetailRow();
        row.setPropertyType(PropertyType.APARTMENT);
        row.setSido("\uC11C\uC6B8\uD2B9\uBCC4\uC2DC");
        row.setSigungu("\uC740\uD3C9\uAD6C");
        row.setLegalDong("\uC9C4\uAD00\uB3D9");
        row.setName("\uC740\uD3C9\uB274\uD0C0\uC6B4 \uC81C\uAC01\uB9D0 \uD478\uB974\uC9C0\uC624(5-4\uB2E8\uC9C0)");

        assertThat(lookup.findHouseholdCount(row)).contains(412);
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
