package com.jiber.backend.community;

import com.jiber.backend.community.dto.CommunityPostListRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityMapper {

    List<CommunityPostRow> findPosts(
            @Param("request") CommunityPostListRequest request,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countPosts(@Param("request") CommunityPostListRequest request);

    CommunityPostRow findPostById(@Param("postId") Long postId);

    int incrementPostViewCount(@Param("postId") Long postId);

    int insertPost(CommunityPostCreateCommand command);

    List<CommunityCommentRow> findCommentsByPostId(@Param("postId") Long postId);

    CommunityCommentRow findCommentById(@Param("commentId") Long commentId);

    int insertComment(CommunityCommentCreateCommand command);
}
