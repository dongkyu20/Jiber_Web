package com.jiber.backend.publicdata.dto;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

public record NormalizedAddress(
        String sido,
        String sigungu,
        String legalDong,
        String jibun,
        String fullAddress,
        String addressKey
) {
}
