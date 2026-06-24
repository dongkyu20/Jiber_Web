package com.jiber.backend.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis.mapper-locations=classpath:/mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
class NoticeMapperMyBatisTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-24T10:00:00+09:00");

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS notices");
        jdbcTemplate.execute("""
                CREATE TABLE notices (
                    notice_id BIGINT AUTO_INCREMENT,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    pinned BOOLEAN NOT NULL DEFAULT FALSE,
                    published_at TIMESTAMP(6) NOT NULL,
                    created_by_user_id BIGINT,
                    updated_by_user_id BIGINT,
                    deleted_at TIMESTAMP(6),
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    PRIMARY KEY (notice_id)
                )
                """);
    }

    @Test
    void publicListReturnsOnlyPublishedAndNotDeletedNoticesWithPinnedFirst() {
        insertNotice(1L, "일반 공지", "본문", false, "2026-06-23T09:00:00+09:00", null);
        insertNotice(2L, "고정 공지", "고정 본문", true, "2026-06-22T09:00:00+09:00", null);
        insertNotice(3L, "예약 공지", "예약 본문", true, "2026-06-25T09:00:00+09:00", null);
        insertNotice(4L, "삭제 공지", "삭제 본문", true, "2026-06-21T09:00:00+09:00", "2026-06-23T10:00:00+09:00");

        var request = new NoticeListRequest(0, 20, "publishedAt,desc", null, null);

        var notices = noticeMapper.findPublicNotices(request, request.effectiveSize(), 0, NOW);

        assertThat(notices).extracting(NoticeRow::noticeId).containsExactly(2L, 1L);
        assertThat(noticeMapper.countPublicNotices(request, NOW)).isEqualTo(2);
    }

    @Test
    void adminListIncludesScheduledNoticesButStillExcludesDeletedRows() {
        insertNotice(1L, "게시 공지", "본문", false, "2026-06-23T09:00:00+09:00", null);
        insertNotice(2L, "예약 공지", "예약 본문", false, "2026-06-25T09:00:00+09:00", null);
        insertNotice(3L, "삭제 공지", "삭제 본문", false, "2026-06-22T09:00:00+09:00", "2026-06-23T10:00:00+09:00");

        var request = new NoticeListRequest(0, 20, "publishedAt,desc", null, null);

        var notices = noticeMapper.findAdminNotices(request, request.effectiveSize(), 0);

        assertThat(notices).extracting(NoticeRow::noticeId).containsExactly(2L, 1L);
        assertThat(noticeMapper.countAdminNotices(request)).isEqualTo(2);
    }

    @Test
    void insertUpdateAndSoftDeleteNotice() {
        var command = new NoticeUpsertCommand(
                null,
                "새 공지",
                "새 공지 본문",
                true,
                OffsetDateTime.parse("2026-06-24T12:00:00+09:00"),
                99L
        );

        noticeMapper.insertNotice(command);

        assertThat(command.noticeId()).isNotNull();
        var created = noticeMapper.findAdminNoticeById(command.noticeId());
        assertThat(created.title()).isEqualTo("새 공지");
        assertThat(created.createdByUserId()).isEqualTo(99L);

        var updated = noticeMapper.updateNotice(new NoticeUpsertCommand(
                command.noticeId(),
                "수정 공지",
                "수정 본문",
                false,
                OffsetDateTime.parse("2026-06-24T13:00:00+09:00"),
                100L
        ));

        assertThat(updated).isEqualTo(1);
        var edited = noticeMapper.findAdminNoticeById(command.noticeId());
        assertThat(edited.title()).isEqualTo("수정 공지");
        assertThat(edited.updatedByUserId()).isEqualTo(100L);

        assertThat(noticeMapper.softDeleteNotice(command.noticeId())).isEqualTo(1);
        assertThat(noticeMapper.findAdminNoticeById(command.noticeId())).isNull();
        assertThat(noticeMapper.softDeleteNotice(command.noticeId())).isZero();
    }

    private void insertNotice(Long noticeId, String title, String content, boolean pinned, String publishedAt, String deletedAt) {
        jdbcTemplate.update("""
                        INSERT INTO notices (
                            notice_id, title, content, pinned, published_at, deleted_at, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                        """,
                noticeId,
                title,
                content,
                pinned,
                OffsetDateTime.parse(publishedAt),
                deletedAt == null ? null : OffsetDateTime.parse(deletedAt)
        );
    }
}
