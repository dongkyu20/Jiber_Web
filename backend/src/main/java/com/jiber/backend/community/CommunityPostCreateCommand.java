package com.jiber.backend.community;

import com.jiber.backend.community.dto.CommunityCategory;

public class CommunityPostCreateCommand {

    private Long postId;
    private final CommunityCategory category;
    private final String title;
    private final String content;
    private final Long authorUserId;
    private final Long relatedPropertyId;

    public CommunityPostCreateCommand(
            CommunityCategory category,
            String title,
            String content,
            Long authorUserId,
            Long relatedPropertyId
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.authorUserId = authorUserId;
        this.relatedPropertyId = relatedPropertyId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public CommunityCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public Long getRelatedPropertyId() {
        return relatedPropertyId;
    }
}
