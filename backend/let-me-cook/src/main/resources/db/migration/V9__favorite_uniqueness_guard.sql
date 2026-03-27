USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v9_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v9_patch()
BEGIN
    DELETE i1
    FROM interactions i1
    INNER JOIN interactions i2
        ON i1.user_id = i2.user_id
        AND i1.recipe_id = i2.recipe_id
        AND i1.interaction_type = 'favorite'
        AND i2.interaction_type = 'favorite'
        AND i1.id > i2.id;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'interactions'
          AND COLUMN_NAME = 'favorite_unique_flag'
    ) THEN
        ALTER TABLE interactions
            ADD COLUMN favorite_unique_flag TINYINT
                GENERATED ALWAYS AS (
                    CASE WHEN interaction_type = 'favorite' THEN 1 ELSE NULL END
                ) STORED;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'interactions'
          AND INDEX_NAME = 'uk_interactions_user_recipe_favorite'
    ) THEN
        CREATE UNIQUE INDEX uk_interactions_user_recipe_favorite
            ON interactions(user_id, recipe_id, favorite_unique_flag);
    END IF;

    UPDATE recipes r
    LEFT JOIN (
        SELECT recipe_id, COUNT(*) AS favorite_count
        FROM interactions
        WHERE interaction_type = 'favorite'
        GROUP BY recipe_id
    ) fav ON fav.recipe_id = r.id
    SET r.favorite_count = COALESCE(fav.favorite_count, 0)
    WHERE COALESCE(r.favorite_count, -1) <> COALESCE(fav.favorite_count, 0);
END$$
DELIMITER ;

CALL sp_apply_foodrec_v9_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v9_patch;
