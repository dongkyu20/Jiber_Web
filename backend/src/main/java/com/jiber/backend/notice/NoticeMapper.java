package com.jiber.backend.notice;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeMapper {

    List<NoticeRow> findPublicNotices(
            @Param("request") NoticeListRequest request,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("now") OffsetDateTime now
    );

    long countPublicNotices(
            @Param("request") NoticeListRequest request,
            @Param("now") OffsetDateTime now
    );

    NoticeRow findPublicNoticeById(
            @Param("noticeId") Long noticeId,
            @Param("now") OffsetDateTime now
    );

    List<NoticeRow> findAdminNotices(
            @Param("request") NoticeListRequest request,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countAdminNotices(@Param("request") NoticeListRequest request);

    NoticeRow findAdminNoticeById(@Param("noticeId") Long noticeId);

    int insertNotice(NoticeUpsertCommand command);

    int updateNotice(NoticeUpsertCommand command);

    int softDeleteNotice(@Param("noticeId") Long noticeId);
}
