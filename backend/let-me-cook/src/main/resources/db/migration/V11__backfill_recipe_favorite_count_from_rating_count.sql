USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v11_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v11_patch()
BEGIN
    /*
      保守恢复口径：
      1. rating_count 视作历史导入收藏基数；
      2. interactions.favorite 视作当前用户收藏增量；
      3. 若库里已有更大的 favorite_count，则保持现值，避免覆盖已修正数据。
    */
    UPDATE recipes r
    LEFT JOIN (
        SELECT recipe_id, COUNT(*) AS favorite_increment
        FROM interactions
        WHERE interaction_type = 'favorite'
        GROUP BY recipe_id
    ) fav ON fav.recipe_id = r.id
    SET r.favorite_count = GREATEST(
        COALESCE(r.favorite_count, 0),
        COALESCE(r.rating_count, 0) + COALESCE(fav.favorite_increment, 0)
    )
    WHERE COALESCE(r.rating_count, 0) > 0
       OR COALESCE(fav.favorite_increment, 0) > 0;
END$$
DELIMITER ;

CALL sp_apply_foodrec_v11_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v11_patch;
