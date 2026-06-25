package com.jiber.backend.community.service;

import com.jiber.backend.common.PageMetadata;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.community.CommunityCommentCreateCommand;
import com.jiber.backend.community.CommunityCommentRow;
import com.jiber.backend.community.CommunityMapper;
import com.jiber.backend.community.CommunityPostCreateCommand;
import com.jiber.backend.community.CommunityPostRow;
import com.jiber.backend.community.dto.CommunityCommentCreateRequest;
import com.jiber.backend.community.dto.CommunityCommentResponse;
import com.jiber.backend.community.dto.CommunityCommentUpdateRequest;
import com.jiber.backend.community.dto.CommunityCategory;
import com.jiber.backend.community.dto.CommunityMutationResponse;
import com.jiber.backend.community.dto.CommunityPostCreateRequest;
import com.jiber.backend.community.dto.CommunityPostDetailResponse;
import com.jiber.backend.community.dto.CommunityPostListRequest;
import com.jiber.backend.community.dto.CommunityPostListResponse;
import com.jiber.backend.community.dto.CommunityPostSummaryResponse;
import com.jiber.backend.community.dto.CommunityPostUpdateRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CommunityService {

    private final CommunityMapper communityMapper;

    public CommunityService(CommunityMapper communityMapper) {
        this.communityMapper = communityMapper;
    }

    public CommunityPostListResponse listPosts(CommunityPostListRequest request) {
        var page = request.effectivePage();
        var size = request.effectiveSize();
        var rows = communityMapper.findPosts(request, size, page * size);
        var total = communityMapper.countPosts(request);
        return new CommunityPostListResponse(
                rows.stream().map(this::toSummary).toList(),
                pageMetadata(page, size, total)
        );
    }

    public CommunityPostDetailResponse getPost(Long postId) {
        var row = findPostOrThrow(postId);
        communityMapper.incrementPostViewCount(postId);
        var refreshed = communityMapper.findPostById(postId);
        return toDetail(refreshed == null ? row : refreshed, communityMapper.findCommentsByPostId(postId));
    }

    public CommunityMutationResponse createPost(CommunityPostCreateRequest request, Long authorUserId) {
        return createPost(request, authorUserId, Set.of());
    }

    public CommunityMutationResponse createPost(CommunityPostCreateRequest request, Long authorUserId, Set<String> roles) {
        requireAuthenticated(authorUserId);
        ensureNoticeWritable(request.category(), roles);
        var command = new CommunityPostCreateCommand(
                request.category(),
                request.title().trim(),
                request.content().trim(),
                authorUserId,
                request.relatedPropertyId()
        );
        communityMapper.insertPost(command);
        return new CommunityMutationResponse(command.getPostId(), "게시글이 등록되었습니다.");
    }

    public CommunityMutationResponse updatePost(Long postId, CommunityPostUpdateRequest request, Long userId) {
        return updatePost(postId, request, userId, Set.of());
    }

    public CommunityMutationResponse updatePost(Long postId, CommunityPostUpdateRequest request, Long userId, Set<String> roles) {
        requireAuthenticated(userId);
        var post = findPostOrThrow(postId);
        ensureOwner(post.authorUserId(), userId);
        ensureNoticeWritable(post.category(), roles);
        ensureNoticeWritable(request.category(), roles);
        communityMapper.updatePost(
                postId,
                request.category(),
                request.title().trim(),
                request.content().trim(),
                request.relatedPropertyId()
        );
        return new CommunityMutationResponse(postId, "게시글이 수정되었습니다.");
    }

    public CommunityMutationResponse deletePost(Long postId, Long userId) {
        requireAuthenticated(userId);
        var post = findPostOrThrow(postId);
        ensureOwner(post.authorUserId(), userId);
        communityMapper.deletePost(postId);
        return new CommunityMutationResponse(postId, "게시글이 삭제되었습니다.");
    }

    public CommunityMutationResponse createComment(Long postId, CommunityCommentCreateRequest request, Long authorUserId) {
        requireAuthenticated(authorUserId);
        findPostOrThrow(postId);
        validateParentComment(postId, request.parentCommentId());
        var command = new CommunityCommentCreateCommand(
                postId,
                request.parentCommentId(),
                authorUserId,
                request.content().trim()
        );
        communityMapper.insertComment(command);
        return new CommunityMutationResponse(command.getCommentId(), "댓글이 등록되었습니다.");
    }

    public CommunityMutationResponse updateComment(
            Long postId,
            Long commentId,
            CommunityCommentUpdateRequest request,
            Long userId
    ) {
        requireAuthenticated(userId);
        findPostOrThrow(postId);
        var comment = findCommentOrThrow(commentId);
        ensureCommentBelongsToPost(postId, comment);
        ensureOwner(comment.authorUserId(), userId);
        communityMapper.updateComment(commentId, request.content().trim());
        return new CommunityMutationResponse(commentId, "댓글이 수정되었습니다.");
    }

    public CommunityMutationResponse deleteComment(Long postId, Long commentId, Long userId) {
        requireAuthenticated(userId);
        findPostOrThrow(postId);
        var comment = findCommentOrThrow(commentId);
        ensureCommentBelongsToPost(postId, comment);
        ensureOwner(comment.authorUserId(), userId);
        communityMapper.deleteComment(commentId);
        return new CommunityMutationResponse(commentId, "댓글이 삭제되었습니다.");
    }

    private void validateParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        var parent = communityMapper.findCommentById(parentCommentId);
        if (parent == null || !postId.equals(parent.postId()) || parent.parentCommentId() != null) {
            throw new ApiException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
        }
    }

    private CommunityPostRow findPostOrThrow(Long postId) {
        var post = communityMapper.findPostById(postId);
        if (post == null) {
            throw new ApiException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
        return post;
    }

    private CommunityCommentRow findCommentOrThrow(Long commentId) {
        var comment = communityMapper.findCommentById(commentId);
        if (comment == null) {
            throw new ApiException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void ensureCommentBelongsToPost(Long postId, CommunityCommentRow comment) {
        if (!postId.equals(comment.postId())) {
            throw new ApiException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
        }
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
    }

    private void ensureOwner(Long authorUserId, Long userId) {
        if (authorUserId == null || !authorUserId.equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void ensureNoticeWritable(CommunityCategory category, Set<String> roles) {
        if (category == CommunityCategory.NOTICE && (roles == null || !roles.contains("ADMIN"))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "공지사항은 관리자만 작성할 수 있습니다.");
        }
    }

    private CommunityPostSummaryResponse toSummary(CommunityPostRow row) {
        return new CommunityPostSummaryResponse(
                row.postId(),
                row.category(),
                row.title(),
                row.authorUserId(),
                row.authorDisplayName(),
                row.viewCount() == null ? 0 : row.viewCount(),
                row.commentCount() == null ? 0 : row.commentCount(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private CommunityPostDetailResponse toDetail(CommunityPostRow row, List<CommunityCommentRow> comments) {
        return new CommunityPostDetailResponse(
                row.postId(),
                row.category(),
                row.title(),
                row.content(),
                row.authorUserId(),
                row.authorDisplayName(),
                row.relatedPropertyId(),
                row.relatedPropertyName(),
                row.relatedPropertyAddress(),
                row.viewCount() == null ? 0 : row.viewCount(),
                row.commentCount() == null ? 0 : row.commentCount(),
                row.createdAt(),
                row.updatedAt(),
                toCommentTree(comments)
        );
    }

    private List<CommunityCommentResponse> toCommentTree(List<CommunityCommentRow> rows) {
        var parents = new LinkedHashMap<Long, MutableComment>();
        var replies = new ArrayList<CommunityCommentRow>();

        for (var row : rows) {
            if (row.parentCommentId() == null) {
                parents.put(row.commentId(), new MutableComment(row));
            } else {
                replies.add(row);
            }
        }

        for (var reply : replies) {
            var parent = parents.get(reply.parentCommentId());
            if (parent != null) {
                parent.replies.add(toComment(reply, List.of()));
            }
        }

        return parents.values().stream()
                .map(parent -> toComment(parent.row, parent.replies))
                .toList();
    }

    private CommunityCommentResponse toComment(CommunityCommentRow row, List<CommunityCommentResponse> replies) {
        return new CommunityCommentResponse(
                row.commentId(),
                row.postId(),
                row.parentCommentId(),
                row.authorUserId(),
                row.authorDisplayName(),
                row.content(),
                row.createdAt(),
                row.updatedAt(),
                replies
        );
    }

    private PageMetadata pageMetadata(int page, int size, long total) {
        if (total == 0) {
            return PageMetadata.empty(page, size);
        }
        return new PageMetadata(page, size, total, (int) Math.ceil((double) total / size));
    }

    private static final class MutableComment {
        private final CommunityCommentRow row;
        private final List<CommunityCommentResponse> replies = new ArrayList<>();

        private MutableComment(CommunityCommentRow row) {
            this.row = row;
        }
    }
}
