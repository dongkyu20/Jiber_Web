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
import com.jiber.backend.community.dto.CommunityMutationResponse;
import com.jiber.backend.community.dto.CommunityPostCreateRequest;
import com.jiber.backend.community.dto.CommunityPostDetailResponse;
import com.jiber.backend.community.dto.CommunityPostListRequest;
import com.jiber.backend.community.dto.CommunityPostListResponse;
import com.jiber.backend.community.dto.CommunityPostSummaryResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        var row = communityMapper.findPostById(postId);
        if (row == null) {
            throw new ApiException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
        communityMapper.incrementPostViewCount(postId);
        var refreshed = communityMapper.findPostById(postId);
        return toDetail(refreshed == null ? row : refreshed, communityMapper.findCommentsByPostId(postId));
    }

    public CommunityMutationResponse createPost(CommunityPostCreateRequest request, Long authorUserId) {
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

    public CommunityMutationResponse createComment(Long postId, CommunityCommentCreateRequest request, Long authorUserId) {
        var post = communityMapper.findPostById(postId);
        if (post == null) {
            throw new ApiException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
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

    private void validateParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        var parent = communityMapper.findCommentById(parentCommentId);
        if (parent == null || !postId.equals(parent.postId()) || parent.parentCommentId() != null) {
            throw new ApiException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
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
