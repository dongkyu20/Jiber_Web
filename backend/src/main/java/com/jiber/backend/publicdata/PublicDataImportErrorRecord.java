package com.jiber.backend.publicdata;

import java.time.YearMonth;

public record PublicDataImportErrorRecord(
        Long importRunId,
        String lawdCd,
        String dealYmd,
        PublicDataApiType apiType,
        String errorCode,
        String message
) {
    public static PublicDataImportErrorRecord of(
            Long importRunId,
            String lawdCd,
            YearMonth dealMonth,
            PublicDataApiType apiType,
            String errorCode,
            String message
    ) {
        return new PublicDataImportErrorRecord(
                importRunId,
                lawdCd,
                dealMonth.toString().replace("-", ""),
                apiType,
                errorCode,
                message
        );
    }
}
