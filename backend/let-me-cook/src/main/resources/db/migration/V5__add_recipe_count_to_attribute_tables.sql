-- 添加 recipe_count 字段到属性表
-- V5__add_recipe_count_to_attribute_tables.sql

-- 为 tastes 表添加 recipe_count 字段
ALTER TABLE tastes ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE tastes ADD INDEX idx_recipe_count (recipe_count);

-- 为 techniques 表添加 recipe_count 字段
ALTER TABLE techniques ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE techniques ADD INDEX idx_recipe_count (recipe_count);

-- 为 time_costs 表添加 recipe_count 字段
ALTER TABLE time_costs ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE time_costs ADD INDEX idx_recipe_count (recipe_count);

-- 为 difficulties 表添加 recipe_count 字段
ALTER TABLE difficulties ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE difficulties ADD INDEX idx_recipe_count (recipe_count);

-- 为 ingredients 表添加 recipe_count 字段
ALTER TABLE ingredients ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE ingredients ADD INDEX idx_recipe_count (recipe_count);

-- 为 categories 表添加 recipe_count 字段
ALTER TABLE categories ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE categories ADD INDEX idx_recipe_count (recipe_count);

-- 初始化各表已有的食谱数量统计
UPDATE tastes t SET recipe_count = (SELECT COUNT(*) FROM recipes r WHERE r.taste_id = t.id);
UPDATE techniques t SET recipe_count = (SELECT COUNT(*) FROM recipes r WHERE r.technique_id = t.id);
UPDATE time_costs t SET recipe_count = (SELECT COUNT(*) FROM recipes r WHERE r.time_cost_id = t.id);
UPDATE difficulties d SET recipe_count = (SELECT COUNT(*) FROM recipes r WHERE r.difficulty_id = d.id);
UPDATE ingredients i SET recipe_count = (SELECT COUNT(DISTINCT recipe_id) FROM recipe_ingredients ri WHERE ri.ingredient_id = i.id);
UPDATE categories c SET recipe_count = (SELECT COUNT(DISTINCT recipe_id) FROM recipe_categories rc WHERE rc.category_id = c.id);
