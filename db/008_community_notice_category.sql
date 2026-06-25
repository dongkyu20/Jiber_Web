SET NAMES utf8mb4;
SET time_zone = '+09:00';

ALTER TABLE community_posts
    MODIFY category ENUM('NOTICE', 'FREE', 'DEAL_REVIEW', 'QNA') NOT NULL;

DROP PROCEDURE IF EXISTS migrate_legacy_notices_to_community;

DELIMITER //
CREATE PROCEDURE migrate_legacy_notices_to_community()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'notices'
    ) THEN
        INSERT INTO community_posts (
            category,
            title,
            content,
            author_user_id,
            view_count,
            created_at,
            updated_at
        )
        SELECT
            'NOTICE',
            n.title,
            n.content,
            n.created_by_user_id,
            0,
            COALESCE(n.published_at, n.created_at),
            n.updated_at
        FROM notices n
        WHERE n.deleted_at IS NULL
          AND NOT EXISTS (
              SELECT 1
              FROM community_posts cp
              WHERE cp.category = 'NOTICE'
                AND cp.title = n.title
                AND cp.content = n.content
                AND cp.created_at = COALESCE(n.published_at, n.created_at)
                AND cp.deleted_at IS NULL
          );

        DROP TABLE notices;
    END IF;
END//
DELIMITER ;

CALL migrate_legacy_notices_to_community();
DROP PROCEDURE IF EXISTS migrate_legacy_notices_to_community;

UPDATE users
SET display_name = '관리자'
WHERE role = 'ADMIN'
  AND (
      display_name IS NULL
      OR display_name = ''
      OR display_name REGEXP '[ÃÂ�]'
      OR display_name LIKE '%ì%'
      OR display_name LIKE '%í%'
      OR display_name LIKE '%ê%'
      OR display_name LIKE '%ë%'
  );
