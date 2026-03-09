-- 优化用户管理列表查询性能的索引迁移脚本
-- 执行时间：2026-03-03

USE food_recommend;

-- 为 comments 表添加复合索引，优化用户评论数统计查询
CREATE INDEX IF NOT EXISTS idx_comment_user_create ON comments(user_id, create_time);

-- 为 interactions 表添加复合索引，优化用户收藏数统计查询
CREATE INDEX IF NOT EXISTS idx_interaction_user_type ON interactions(user_id, interaction_type);

-- 为 users 表添加状态索引，优化按状态筛选查询
CREATE INDEX IF NOT EXISTS idx_user_status ON users(status);

-- 为 users 表添加创建时间索引，优化默认排序查询
CREATE INDEX IF NOT EXISTS idx_user_create_time ON users(create_time DESC);

-- 分析表以更新统计信息
ANALYZE TABLE users;
ANALYZE TABLE comments;
ANALYZE TABLE interactions;
