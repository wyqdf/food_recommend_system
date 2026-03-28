USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v8_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v8_patch()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'recipes'
          AND INDEX_NAME = 'idx_recipes_status_favorite_count'
    ) THEN
        CREATE INDEX idx_recipes_status_favorite_count
            ON recipes(status, favorite_count DESC, id DESC);
    END IF;
    /* 仅补齐收藏热度索引；recipes.favorite_count 保留导入基数，后续由收藏业务实时增减。 */
END$$
DELIMITER ;

CALL sp_apply_foodrec_v8_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v8_patch;
