package com.jiber.backend.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.community.dto.CommunityCategory;
import com.jiber.backend.community.dto.CommunityCommentCreateRequest;
import com.jiber.backend.community.dto.CommunityPostListRequest;
import com.jiber.backend.community.service.CommunityService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-25T10:00:00+09:00");

    @Test
    void listPostsReturnsPagedSummaries() {
        var mapper = new FakeCommunityMapper();
        mapper.posts.add(postRow(1L, "First post", 3L, 2L));
        mapper.total = 1;
        var service = new CommunityService(mapper);

        var response = service.listPosts(new CommunityPostListRequest(0, 20, "createdAt,desc", null, null));

        assertThat(response.items()).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(1L);
            assertThat(post.title()).isEqualTo("First post");
            assertThat(post.viewCount()).isEqualTo(3L);
            assertThat(post.commentCount()).isEqualTo(2L);
        });
        assertThat(response.page().totalElements()).isEqualTo(1);
    }

    @Test
    void getPostIncrementsViewCountAndBuildsCommentTree() {
        var mapper = new FakeCommunityMapper();
        mapper.post = postRow(1L, "Detail", 0L, 2L);
        mapper.comments.add(commentRow(10L, null, "parent"));
        mapper.comments.add(commentRow(11L, 10L, "reply"));
        var service = new CommunityService(mapper);

        var response = service.getPost(1L);

        assertThat(mapper.incrementedPostId).isEqualTo(1L);
        assertThat(response.comments()).singleElement().satisfies(comment -> {
            assertThat(comment.content()).isEqualTo("parent");
            assertThat(comment.replies()).singleElement().satisfies(reply ->
                    assertThat(reply.content()).isEqualTo("reply"));
        });
    }

    @Test
    void createCommentRejectsMissingPost() {
        var service = new CommunityService(new FakeCommunityMapper());

        assertThatThrownBy(() -> service.createComment(404L, new CommunityCommentCreateRequest(null, "body"), 7L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND));
    }

    private static CommunityPostRow postRow(Long postId, String title, Long viewCount, Long commentCount) {
        return new CommunityPostRow(
                postId,
                CommunityCategory.FREE,
                title,
                "content",
                7L,
                "Author",
                null,
                null,
                null,
                viewCount,
                commentCount,
                NOW,
                NOW
        );
    }

    private static CommunityCommentRow commentRow(Long commentId, Long parentCommentId, String content) {
        return new CommunityCommentRow(commentId, 1L, parentCommentId, 7L, "Author", content, NOW, NOW);
    }

    private static class FakeCommunityMapper implements CommunityMapper {

        private final List<CommunityPostRow> posts = new ArrayList<>();
        private final List<CommunityCommentRow> comments = new ArrayList<>();
        private CommunityPostRow post;
        private CommunityCommentRow comment;
        private long total;
        private Long incrementedPostId;

        @Override
        public List<CommunityPostRow> findPosts(CommunityPostListRequest request, int limit, int offset) {
            return posts;
        }

        @Override
        public long countPosts(CommunityPostListRequest request) {
            return total;
        }

        @Override
        public CommunityPostRow findPostById(Long postId) {
            return post;
        }

        @Override
        public int incrementPostViewCount(Long postId) {
            incrementedPostId = postId;
            return 1;
        }

        @Override
        public int insertPost(CommunityPostCreateCommand command) {
            command.setPostId(100L);
            return 1;
        }

        @Override
        public List<CommunityCommentRow> findCommentsByPostId(Long postId) {
            return comments;
        }

        @Override
        public CommunityCommentRow findCommentById(Long commentId) {
            return comment;
        }

        @Override
        public int insertComment(CommunityCommentCreateCommand command) {
            command.setCommentId(200L);
            return 1;
        }
    }
}
