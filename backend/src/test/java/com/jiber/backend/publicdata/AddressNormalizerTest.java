package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressNormalizerTest {

    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void buildsFullAddressAndStableAddressKeyFromJibun() {
        var address = normalizer.normalize(" 서울특별시 ", "강남구", " 역삼동 ", " 12-3 ");

        assertThat(address.fullAddress()).isEqualTo("서울특별시 강남구 역삼동 12-3");
        assertThat(address.addressKey()).isEqualTo("서울특별시|강남구|역삼동|12-3");
    }
}
