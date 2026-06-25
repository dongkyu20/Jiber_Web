package com.jiber.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityPostCreateRequest(
        @NotNull CommunityCategory category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        Long relatedPropertyId
) {
}
