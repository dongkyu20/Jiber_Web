package com.jiber.backend.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NoticeServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-24T01:00:00Z");

    private NoticeMapper noticeMapper;
    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        noticeMapper = mock(NoticeMapper.class);
        noticeService = NoticeService.forTesting(
                noticeMapper,
                Clock.fixed(Instant.parse("2026-06-24T01:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void listPublicNoticesMapsRowsToSummaryAndPagination() {
        var request = new NoticeListRequest(0, 20, "publishedAt,desc", "점검", null);
        when(noticeMapper.findPublicNotices(eq(request), eq(20), eq(0), eq(NOW)))
                .thenReturn(List.of(row(10L, "점검 안내", "서비스 점검을 진행합니다.\n자세한 일정은 본문을 확인하세요.", true)));
        when(noticeMapper.countPublicNotices(request, NOW)).thenReturn(1L);

        var response = noticeService.listNotices(request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).noticeId()).isEqualTo(10L);
        assertThat(response.items().get(0).summary()).isEqualTo("서비스 점검을 진행합니다. 자세한 일정은 본문을 확인하세요.");
        assertThat(response.page().totalElements()).isEqualTo(1L);
    }

    @Test
    void listAdminNoticesIncludesRowsFromAdminMapper() {
        var request = new NoticeListRequest(0, 20, "publishedAt,desc", null, null);
        when(noticeMapper.findAdminNotices(request, 20, 0)).thenReturn(List.of(row(11L, "예약 안내", "예약 본문", false)));
        when(noticeMapper.countAdminNotices(request)).thenReturn(1L);

        var response = noticeService.listAdminNotices(request);

        assertThat(response.items()).extracting(NoticeSummaryResponse::noticeId).containsExactly(11L);
        verify(noticeMapper).findAdminNotices(request, 20, 0);
    }

    @Test
    void publicDetailThrowsNoticeNotFoundWhenMapperReturnsNull() {
        when(noticeMapper.findPublicNoticeById(99L, NOW)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.getNotice(99L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    void createNoticePassesActorUserIdToMapperAndReturnsGeneratedId() {
        var request = request("새 공지", "새 공지 본문");

        when(noticeMapper.insertNotice(any())).thenAnswer(invocation -> {
            NoticeUpsertCommand command = invocation.getArgument(0);
            command.setNoticeId(123L);
            return 1;
        });

        var response = noticeService.createNotice(request, 7L);

        var captor = ArgumentCaptor.forClass(NoticeUpsertCommand.class);
        verify(noticeMapper).insertNotice(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isEqualTo(7L);
        assertThat(response.noticeId()).isEqualTo(123L);
    }

    @Test
    void updateAndDeleteThrowNoticeNotFoundWhenNoActiveRowChanged() {
        var request = request("수정 공지", "수정 공지 본문");
        when(noticeMapper.updateNotice(any())).thenReturn(0);
        when(noticeMapper.softDeleteNotice(99L)).thenReturn(0);

        assertThatThrownBy(() -> noticeService.updateNotice(99L, request, 7L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        assertThatThrownBy(() -> noticeService.deleteNotice(99L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    private NoticeUpsertRequest request(String title, String content) {
        return new NoticeUpsertRequest(title, content, false, OffsetDateTime.parse("2026-06-24T12:00:00+09:00"));
    }

    private NoticeRow row(Long noticeId, String title, String content, boolean pinned) {
        return new NoticeRow(
                noticeId,
                title,
                content,
                pinned,
                OffsetDateTime.parse("2026-06-24T09:00:00+09:00"),
                null,
                null,
                OffsetDateTime.parse("2026-06-24T08:00:00+09:00"),
                OffsetDateTime.parse("2026-06-24T08:30:00+09:00")
        );
    }
}
