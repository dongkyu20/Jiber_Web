package com.jiber.backend.notice;

import java.time.OffsetDateTime;

public class NoticeUpsertCommand {

    private Long noticeId;
    private final String title;
    private final String content;
    private final boolean pinned;
    private final OffsetDateTime publishedAt;
    private final Long actorUserId;

    public NoticeUpsertCommand(
            Long noticeId,
            String title,
            String content,
            boolean pinned,
            OffsetDateTime publishedAt,
            Long actorUserId
    ) {
        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.publishedAt = publishedAt;
        this.actorUserId = actorUserId;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public Long noticeId() {
        return noticeId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getActorUserId() {
        return actorUserId;
    }
}
