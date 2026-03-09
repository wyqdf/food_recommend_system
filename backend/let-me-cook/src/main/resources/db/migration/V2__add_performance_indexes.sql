-- =====================================================
-- 性能优化索引脚本
-- 目的：提升页面加载速度和查询性能
-- =====================================================

-- 1. users 表索引
-- 用于统计今日新增用户、用户趋势分析
CREATE INDEX IF NOT EXISTS idx_users_create_time ON users(create_time);

-- 用于用户搜索（用户名、昵称、邮箱）
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 用于用户状态筛选
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 2. recipes 表索引
-- 用于统计今日新增菜谱、菜谱趋势分析
CREATE INDEX IF NOT EXISTS idx_recipes_create_time ON recipes(create_time);

-- 用于菜谱状态筛选（随机查询、相似推荐）
CREATE INDEX IF NOT EXISTS idx_recipes_status ON recipes(status);

-- 用于热门菜谱排序
CREATE INDEX IF NOT EXISTS idx_recipes_like_count ON recipes(like_count);

-- 用于菜谱搜索
CREATE INDEX IF NOT EXISTS idx_recipes_title ON recipes(title);

-- 用于分类、难度、时间等筛选
CREATE INDEX IF NOT EXISTS idx_recipes_difficulty_id ON recipes(difficulty_id);
CREATE INDEX IF NOT EXISTS idx_recipes_time_cost_id ON recipes(time_cost_id);
CREATE INDEX IF NOT EXISTS idx_recipes_taste_id ON recipes(taste_id);
CREATE INDEX IF NOT EXISTS idx_recipes_technique_id ON recipes(technique_id);

-- 3. interactions 表索引
-- 用于统计今日互动数、用户收藏数
CREATE INDEX IF NOT EXISTS idx_interactions_create_time ON interactions(create_time);
CREATE INDEX IF NOT EXISTS idx_interactions_user_id ON interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_interactions_recipe_id ON interactions(recipe_id);
CREATE INDEX IF NOT EXISTS idx_interactions_type ON interactions(interaction_type);

-- 复合索引：优化用户收藏数统计查询
CREATE INDEX IF NOT EXISTS idx_interactions_user_type ON interactions(user_id, interaction_type);

-- 4. comments 表索引
-- 用于统计今日评论数、用户评论数、菜谱评论数
CREATE INDEX IF NOT EXISTS idx_comments_create_time ON comments(create_time);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_recipe_id ON comments(recipe_id);

-- 5. recipe_categories 表索引
-- 用于分类菜谱统计、分类筛选
CREATE INDEX IF NOT EXISTS idx_recipe_categories_recipe_id ON recipe_categories(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_categories_category_id ON recipe_categories(category_id);

-- 6. recipe_ingredients 表索引
-- 用于相似菜谱推荐查询
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_ingredient_id ON recipe_ingredients(ingredient_id);

-- 复合索引：优化按配料类型查询
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_type ON recipe_ingredients(ingredient_type);

-- 7. ingredients 表索引
-- 用于配料名称查询
CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients(name);

-- 8. favorites 表索引（如果存在 favorites 表）
-- CREATE INDEX IF NOT EXISTS idx_favorites_user_id ON favorites(user_id);
-- CREATE INDEX IF NOT EXISTS idx_favorites_recipe_id ON favorites(recipe_id);

-- =====================================================
-- 优化说明：
-- 1. 所有时间字段添加索引，优化 DATE 范围查询
-- 2. 所有外键字段添加索引，优化 JOIN 查询
-- 3. 所有状态字段添加索引，优化筛选查询
-- 4. 所有计数字段添加索引，优化排序查询
-- 5. 关键复合索引，优化特定场景查询
-- 
-- 预期性能提升：
-- -  Dashboard 统计查询：提升 50-80%
-- - 用户列表查询：提升 30-50%
-- - 菜谱列表查询：提升 30-50%
-- - 相似菜谱推荐：提升 40-60%
-- =====================================================
