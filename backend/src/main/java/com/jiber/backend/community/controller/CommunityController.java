package com.jiber.backend.community.controller;

import com.jiber.backend.auth.dto.AuthUserPrincipal;
import com.jiber.backend.community.dto.CommunityCommentCreateRequest;
import com.jiber.backend.community.dto.CommunityCommentUpdateRequest;
import com.jiber.backend.community.dto.CommunityMutationResponse;
import com.jiber.backend.community.dto.CommunityPostCreateRequest;
import com.jiber.backend.community.dto.CommunityPostDetailResponse;
import com.jiber.backend.community.dto.CommunityPostListRequest;
import com.jiber.backend.community.dto.CommunityPostListResponse;
import com.jiber.backend.community.dto.CommunityPostUpdateRequest;
import com.jiber.backend.community.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public CommunityPostListResponse listPosts(@Valid @ModelAttribute CommunityPostListRequest request) {
        return communityService.listPosts(request);
    }

    @GetMapping("/{postId}")
    public CommunityPostDetailResponse getPost(@PathVariable Long postId) {
        return communityService.getPost(postId);
    }

    @PostMapping
    public CommunityMutationResponse createPost(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody CommunityPostCreateRequest request
    ) {
        return communityService.createPost(request, userId(principal), roles(principal));
    }

    @PutMapping("/{postId}")
    public CommunityMutationResponse updatePost(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityPostUpdateRequest request
    ) {
        return communityService.updatePost(postId, request, userId(principal), roles(principal));
    }

    @DeleteMapping("/{postId}")
    public CommunityMutationResponse deletePost(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long postId
    ) {
        return communityService.deletePost(postId, userId(principal));
    }

    @PostMapping("/{postId}/comments")
    public CommunityMutationResponse createComment(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentCreateRequest request
    ) {
        return communityService.createComment(postId, request, userId(principal));
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public CommunityMutationResponse updateComment(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommunityCommentUpdateRequest request
    ) {
        return communityService.updateComment(postId, commentId, request, userId(principal));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public CommunityMutationResponse deleteComment(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return communityService.deleteComment(postId, commentId, userId(principal));
    }

    private Long userId(AuthUserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }

    private java.util.Set<String> roles(AuthUserPrincipal principal) {
        return principal == null ? java.util.Set.of() : principal.roles();
    }
}
