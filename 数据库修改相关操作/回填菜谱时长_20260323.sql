-- ========================================
-- 菜谱时长批量回填（支持回滚）
-- 时间: 2026-03-23
-- 目标:
-- 1) 给每个菜谱写入一个大概时长
-- 2) 提升“打工人速食”模式命中（优先短时长）
-- ========================================

USE food_recommend;
SET NAMES utf8mb4;

START TRANSACTION;

-- 0) 先做备份，便于回滚
CREATE TABLE IF NOT EXISTS recipe_time_cost_backup_20260323 AS
SELECT id, time_cost_id, NOW() AS backup_time
FROM recipes;

-- 1) 补齐时长字典（已存在则忽略）
INSERT IGNORE INTO time_costs(name) VALUES
('10分钟'),
('15分钟'),
('20分钟'),
('30分钟'),
('45分钟'),
('1小时'),
('2小时');

-- 2) 取各时长 ID
SET @t10 := (SELECT id FROM time_costs WHERE name = '10分钟' LIMIT 1);
SET @t15 := (SELECT id FROM time_costs WHERE name = '15分钟' LIMIT 1);
SET @t20 := (SELECT id FROM time_costs WHERE name = '20分钟' LIMIT 1);
SET @t30 := (SELECT id FROM time_costs WHERE name = '30分钟' LIMIT 1);
SET @t45 := (SELECT id FROM time_costs WHERE name = '45分钟' LIMIT 1);
SET @t60 := (SELECT id FROM time_costs WHERE name = '1小时' LIMIT 1);
SET @t120 := (SELECT id FROM time_costs WHERE name = '2小时' LIMIT 1);

-- 3) 全量估算时长（按标题 + 分类关键词）
--    规则：快手/便当/早餐/一人食 -> 10~20
--         减脂/轻食/沙拉 -> 20
--         家常/下饭 -> 30
--         宴客/聚会/硬菜/烘焙/炖煮 -> 45~120
UPDATE recipes r
SET r.time_cost_id = (
  CASE
    -- 快速餐（打工人优先）
    WHEN r.title LIKE '%快手%' OR r.title LIKE '%速食%' OR r.title LIKE '%一人食%' OR r.title LIKE '%便当%'
      OR r.title LIKE '%早餐%' OR r.title LIKE '%懒人%'
      OR EXISTS (
        SELECT 1
        FROM recipe_categories rc
        INNER JOIN categories c ON c.id = rc.category_id
        WHERE rc.recipe_id = r.id
          AND (c.name LIKE '%快手%' OR c.name LIKE '%一人食%' OR c.name LIKE '%便当%' OR c.name LIKE '%早餐%')
      )
    THEN COALESCE(@t15, @t20, @t10, @t30)

    -- 健身减脂（通常 20 分钟上下）
    WHEN r.title LIKE '%减脂%' OR r.title LIKE '%健身%' OR r.title LIKE '%轻食%' OR r.title LIKE '%沙拉%' OR r.title LIKE '%低卡%'
      OR EXISTS (
        SELECT 1
        FROM recipe_categories rc
        INNER JOIN categories c ON c.id = rc.category_id
        WHERE rc.recipe_id = r.id
          AND (c.name LIKE '%减脂%' OR c.name LIKE '%健身%' OR c.name LIKE '%轻食%' OR c.name LIKE '%沙拉%')
      )
    THEN COALESCE(@t20, @t15, @t30)

    -- 聚会/宴客/大制作
    WHEN r.title LIKE '%聚会%' OR r.title LIKE '%宴客%' OR r.title LIKE '%硬菜%' OR r.title LIKE '%烘焙%'
      OR r.title LIKE '%招待%' OR r.title LIKE '%派对%' OR r.title LIKE '%炖%' OR r.title LIKE '%卤%' OR r.title LIKE '%焖%'
      OR EXISTS (
        SELECT 1
        FROM recipe_categories rc
        INNER JOIN categories c ON c.id = rc.category_id
        WHERE rc.recipe_id = r.id
          AND (c.name LIKE '%宴客%' OR c.name LIKE '%聚会%' OR c.name LIKE '%烘焙%' OR c.name LIKE '%甜点%')
      )
    THEN COALESCE(@t60, @t45, @t120, @t30)

    -- 家庭/家常
    WHEN r.title LIKE '%家常%' OR r.title LIKE '%家庭%' OR r.title LIKE '%下饭%' OR r.title LIKE '%全家%'
      OR EXISTS (
        SELECT 1
        FROM recipe_categories rc
        INNER JOIN categories c ON c.id = rc.category_id
        WHERE rc.recipe_id = r.id
          AND (c.name LIKE '%家常%' OR c.name LIKE '%家庭%' OR c.name LIKE '%下饭%')
      )
    THEN COALESCE(@t30, @t20, @t45)

    -- 默认给 30 分钟
    ELSE COALESCE(@t30, @t20, @t45)
  END
);

COMMIT;

-- 4) 检查分布
SELECT tc.name AS time_name, COUNT(*) AS cnt
FROM recipes r
LEFT JOIN time_costs tc ON tc.id = r.time_cost_id
GROUP BY tc.name
ORDER BY cnt DESC;

-- 5) 回滚（需要时手动执行）
-- START TRANSACTION;
-- UPDATE recipes r
-- INNER JOIN recipe_time_cost_backup_20260323 b ON b.id = r.id
-- SET r.time_cost_id = b.time_cost_id;
-- COMMIT;
