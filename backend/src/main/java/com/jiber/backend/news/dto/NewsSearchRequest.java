package com.jiber.backend.news.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record NewsSearchRequest(
        @Size(max = 80, message = "검색어는 80자 이하로 입력해 주세요.")
        String query,
        @Positive @Max(50)
        Integer display
) {
    public String effectiveQuery() {
        if (!StringUtils.hasText(query)) {
            return "부동산";
        }
        return query.trim();
    }

    public int effectiveDisplay() {
        return display == null ? 20 : display;
    }
}
