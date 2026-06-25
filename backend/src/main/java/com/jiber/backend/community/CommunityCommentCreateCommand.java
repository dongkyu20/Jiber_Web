package com.jiber.backend.community;

public class CommunityCommentCreateCommand {

    private Long commentId;
    private final Long postId;
    private final Long parentCommentId;
    private final Long authorUserId;
    private final String content;

    public CommunityCommentCreateCommand(Long postId, Long parentCommentId, Long authorUserId, String content) {
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.authorUserId = authorUserId;
        this.content = content;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getContent() {
        return content;
    }
}
