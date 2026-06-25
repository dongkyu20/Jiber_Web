package com.jiber.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityCommentCreateRequest(
        Long parentCommentId,
        @NotBlank @Size(max = 5000) String content
) {
}
