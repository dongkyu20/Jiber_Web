package com.jiber.backend.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LawdCodeRegistryTest {

    private final LawdCodeRegistry registry = new LawdCodeRegistry();

    @Test
    void loadsSeoulAndBusanSigunguLawdCodes() {
        var seoulCodes = registry.findByRegions(List.of(PublicDataTargetRegion.SEOUL));
        var busanCodes = registry.findByRegions(List.of(PublicDataTargetRegion.BUSAN));

        assertThat(seoulCodes).hasSize(25);
        assertThat(busanCodes).hasSize(16);
        assertThat(seoulCodes)
                .anySatisfy(code -> {
                    assertThat(code.lawdCd()).isEqualTo("11680");
                    assertThat(code.sido()).isEqualTo("서울특별시");
                    assertThat(code.sigungu()).isEqualTo("강남구");
                });
        assertThat(busanCodes)
                .anySatisfy(code -> {
                    assertThat(code.lawdCd()).isEqualTo("26350");
                    assertThat(code.sido()).isEqualTo("부산광역시");
                    assertThat(code.sigungu()).isEqualTo("해운대구");
                });
    }
}
