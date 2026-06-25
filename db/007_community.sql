SET NAMES utf8mb4;
SET time_zone = '+09:00';

CREATE TABLE IF NOT EXISTS community_posts (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    category ENUM('FREE', 'DEAL_REVIEW', 'QNA') NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_user_id BIGINT NULL,
    related_property_id BIGINT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (post_id),
    KEY idx_community_posts_list (deleted_at, category, created_at DESC),
    KEY idx_community_posts_author (author_user_id, created_at DESC),
    KEY idx_community_posts_property (related_property_id, created_at DESC),
    FULLTEXT KEY ft_community_posts_title_content (title, content),
    CONSTRAINT fk_community_posts_author
        FOREIGN KEY (author_user_id) REFERENCES users (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_community_posts_property
        FOREIGN KEY (related_property_id) REFERENCES properties (property_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    author_user_id BIGINT NULL,
    content TEXT NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (comment_id),
    KEY idx_community_comments_post (post_id, deleted_at, created_at),
    KEY idx_community_comments_parent (parent_comment_id, created_at),
    KEY idx_community_comments_author (author_user_id, created_at DESC),
    CONSTRAINT fk_community_comments_post
        FOREIGN KEY (post_id) REFERENCES community_posts (post_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES community_comments (comment_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_author
        FOREIGN KEY (author_user_id) REFERENCES users (user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
