USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v7_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v7_patch()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'interactions'
          AND INDEX_NAME = 'idx_interactions_user_recipe_type'
    ) THEN
        CREATE INDEX idx_interactions_user_recipe_type
            ON interactions(user_id, recipe_id, interaction_type);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'interactions'
          AND INDEX_NAME = 'idx_interactions_user_type_time_recipe'
    ) THEN
        CREATE INDEX idx_interactions_user_type_time_recipe
            ON interactions(user_id, interaction_type, create_time DESC, id DESC, recipe_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'recipe_ingredients'
          AND INDEX_NAME = 'idx_recipe_ingredients_ing_type_recipe'
    ) THEN
        CREATE INDEX idx_recipe_ingredients_ing_type_recipe
            ON recipe_ingredients(ingredient_id, ingredient_type, recipe_id);
    END IF;
END$$
DELIMITER ;

CALL sp_apply_foodrec_v7_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v7_patch;
