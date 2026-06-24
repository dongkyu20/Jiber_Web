package com.jiber.backend.notice;

import com.jiber.backend.common.PageMetadata;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoticeService {

    private static final int SUMMARY_MAX_LENGTH = 120;

    private final NoticeMapper noticeMapper;
    private final Clock clock;

    @Autowired
    public NoticeService(NoticeMapper noticeMapper) {
        this(noticeMapper, Clock.systemUTC());
    }

    public static NoticeService forTesting(NoticeMapper noticeMapper, Clock clock) {
        return new NoticeService(noticeMapper, clock);
    }

    private NoticeService(NoticeMapper noticeMapper, Clock clock) {
        this.noticeMapper = noticeMapper;
        this.clock = clock;
    }

    public NoticeListResponse listNotices(NoticeListRequest request) {
        var page = request.effectivePage();
        var size = request.effectiveSize();
        var now = now();
        var rows = noticeMapper.findPublicNotices(request, size, page * size, now);
        var total = noticeMapper.countPublicNotices(request, now);
        return new NoticeListResponse(
                rows.stream().map(this::toSummary).toList(),
                pageMetadata(page, size, total)
        );
    }

    public NoticeDetailResponse getNotice(Long noticeId) {
        var row = noticeMapper.findPublicNoticeById(noticeId, now());
        if (row == null) {
            throw new ApiException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return toDetail(row);
    }

    public NoticeListResponse listAdminNotices(NoticeListRequest request) {
        var page = request.effectivePage();
        var size = request.effectiveSize();
        var rows = noticeMapper.findAdminNotices(request, size, page * size);
        var total = noticeMapper.countAdminNotices(request);
        return new NoticeListResponse(
                rows.stream().map(this::toSummary).toList(),
                pageMetadata(page, size, total)
        );
    }

    public NoticeDetailResponse getAdminNotice(Long noticeId) {
        var row = noticeMapper.findAdminNoticeById(noticeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return toDetail(row);
    }

    public NoticeMutationResponse createNotice(NoticeUpsertRequest request) {
        return createNotice(request, null);
    }

    public NoticeMutationResponse createNotice(NoticeUpsertRequest request, Long actorUserId) {
        var command = toCommand(null, request, actorUserId);
        noticeMapper.insertNotice(command);
        return new NoticeMutationResponse(command.getNoticeId(), "공지사항을 등록했습니다.");
    }

    public NoticeMutationResponse updateNotice(Long noticeId, NoticeUpsertRequest request) {
        return updateNotice(noticeId, request, null);
    }

    public NoticeMutationResponse updateNotice(Long noticeId, NoticeUpsertRequest request, Long actorUserId) {
        var updated = noticeMapper.updateNotice(toCommand(noticeId, request, actorUserId));
        if (updated == 0) {
            throw new ApiException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return new NoticeMutationResponse(noticeId, "공지사항을 수정했습니다.");
    }

    public NoticeMutationResponse deleteNotice(Long noticeId) {
        var deleted = noticeMapper.softDeleteNotice(noticeId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return new NoticeMutationResponse(noticeId, "공지사항을 삭제했습니다.");
    }

    private NoticeUpsertCommand toCommand(Long noticeId, NoticeUpsertRequest request, Long actorUserId) {
        return new NoticeUpsertCommand(
                noticeId,
                request.title().trim(),
                request.content().trim(),
                Boolean.TRUE.equals(request.pinned()),
                request.publishedAt(),
                actorUserId
        );
    }

    private NoticeSummaryResponse toSummary(NoticeRow row) {
        return new NoticeSummaryResponse(
                row.noticeId(),
                row.title(),
                summary(row.content()),
                Boolean.TRUE.equals(row.pinned()),
                row.publishedAt(),
                row.createdAt()
        );
    }

    private NoticeDetailResponse toDetail(NoticeRow row) {
        return new NoticeDetailResponse(
                row.noticeId(),
                row.title(),
                row.content(),
                Boolean.TRUE.equals(row.pinned()),
                row.publishedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private String summary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        var compact = content.trim().replaceAll("\\s+", " ");
        if (compact.length() <= SUMMARY_MAX_LENGTH) {
            return compact;
        }
        return compact.substring(0, SUMMARY_MAX_LENGTH - 1) + "…";
    }

    private PageMetadata pageMetadata(int page, int size, long total) {
        if (total == 0) {
            return PageMetadata.empty(page, size);
        }
        return new PageMetadata(page, size, total, (int) Math.ceil((double) total / size));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
