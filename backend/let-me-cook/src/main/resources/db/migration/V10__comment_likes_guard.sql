USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v10_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v10_patch()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'comment_likes'
    ) THEN
        CREATE TABLE comment_likes (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            comment_id INT NOT NULL,
            user_id INT NOT NULL,
            create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_comment_like_comment
                FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
            CONSTRAINT fk_comment_like_user
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            UNIQUE KEY uk_comment_like_user_comment (user_id, comment_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞明细表';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'comment_likes'
          AND INDEX_NAME = 'idx_comment_likes_comment_time'
    ) THEN
        CREATE INDEX idx_comment_likes_comment_time
            ON comment_likes(comment_id, create_time DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'comment_likes'
          AND INDEX_NAME = 'idx_comment_likes_user_time'
    ) THEN
        CREATE INDEX idx_comment_likes_user_time
            ON comment_likes(user_id, create_time DESC);
    END IF;
END$$
DELIMITER ;

CALL sp_apply_foodrec_v10_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v10_patch;
