-- 创建数据库
CREATE DATABASE IF NOT EXISTS food_recommend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE food_recommend;

-- ===================== 基础映射表 =====================

-- 分类表（热菜、家常菜、私房菜等）
CREATE TABLE IF NOT EXISTS categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- 口味表（原味、咸鲜、甜味等）
CREATE TABLE IF NOT EXISTS tastes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口味表';

-- 工艺表（炒、焖、煮、蒸等）
CREATE TABLE IF NOT EXISTS techniques (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺表';

-- 耗时表（廿分钟、一小时等）
CREATE TABLE IF NOT EXISTS time_costs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗时表';

-- 难度表（普通、简单、高级等）
CREATE TABLE IF NOT EXISTS difficulties (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='难度表';

-- 食材表
CREATE TABLE IF NOT EXISTS ingredients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';

-- 厨具表
CREATE TABLE IF NOT EXISTS cookwares (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    recipe_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厨具表';

-- ===================== 核心业务表 =====================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_id INT COMMENT '原始用户ID',
    username VARCHAR(100) UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    avatar VARCHAR(255),
    last_login_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_old_id (old_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户冷启动画像表
CREATE TABLE IF NOT EXISTS user_preference_profiles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    diet_goal VARCHAR(50) COMMENT '饮食目标',
    cooking_skill VARCHAR(50) COMMENT '烹饪水平',
    time_budget VARCHAR(50) COMMENT '可投入烹饪时长',
    preferred_tastes_json JSON COMMENT '口味偏好',
    taboo_ingredients_json JSON COMMENT '忌口/过敏',
    available_cookwares_json JSON COMMENT '可用厨具',
    preferred_scenes_json JSON COMMENT '场景偏好',
    onboarding_completed TINYINT DEFAULT 0 COMMENT '冷启动问卷是否完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好画像表';

-- 菜谱表
CREATE TABLE IF NOT EXISTS recipes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_id INT COMMENT '原始菜谱ID',
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    author_uid VARCHAR(50),
    description TEXT,
    tips TEXT,
    cookware VARCHAR(200),
    image VARCHAR(255),
    taste_id INT COMMENT '口味ID',
    technique_id INT COMMENT '工艺ID',
    time_cost_id INT COMMENT '耗时ID',
    difficulty_id INT COMMENT '难度ID',
    reply_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    rating_count INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (taste_id) REFERENCES tastes(id),
    FOREIGN KEY (technique_id) REFERENCES techniques(id),
    FOREIGN KEY (time_cost_id) REFERENCES time_costs(id),
    FOREIGN KEY (difficulty_id) REFERENCES difficulties(id),
    UNIQUE KEY uk_old_id (old_id),
    KEY idx_recipe_author_uid (author_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱表';

-- 菜谱-分类关联表
CREATE TABLE IF NOT EXISTS recipe_categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipe_id INT NOT NULL,
    category_id INT NOT NULL,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    UNIQUE KEY uk_recipe_category (recipe_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱-分类关联表';

-- 菜谱-食材关联表
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipe_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    ingredient_type ENUM('main', 'sub', 'seasoning') COMMENT '食材类型：主料/辅料/调料',
    quantity VARCHAR(100) COMMENT '用量',
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱-食材关联表';

-- 烹饪步骤表
CREATE TABLE IF NOT EXISTS cooking_steps (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipe_id INT NOT NULL,
    step_number INT NOT NULL,
    description TEXT NOT NULL,
    image VARCHAR(255),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烹饪步骤表';

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_id INT COMMENT '原始评论ID',
    recipe_id INT NOT NULL,
    user_id INT,
    content TEXT,
    publish_time DATETIME,
    likes INT DEFAULT 0,
    is_reply TINYINT DEFAULT 0,
    reply_to_user VARCHAR(100),
    device VARCHAR(50),
    has_picture TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 评论点赞明细表
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_like_user_comment (user_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞明细表';

-- 用户互动表（点赞、收藏等）
CREATE TABLE IF NOT EXISTS interactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    recipe_id INT NOT NULL,
    interaction_type ENUM('like', 'favorite', 'view') NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    favorite_unique_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN interaction_type = 'favorite' THEN 1 ELSE NULL END
    ) STORED,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_interactions_user_recipe_favorite (user_id, recipe_id, favorite_unique_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户互动表';

-- 行为埋点事件表
CREATE TABLE IF NOT EXISTS behavior_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    session_id VARCHAR(64) NOT NULL,
    recipe_id INT NULL,
    event_type VARCHAR(64) NOT NULL,
    source_page VARCHAR(64) NULL,
    scene_code VARCHAR(32) NULL,
    step_number INT NULL,
    duration_ms INT NULL,
    extra_json JSON NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为埋点事件表';

-- 烹饪会话表
CREATE TABLE IF NOT EXISTS cooking_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    recipe_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress/completed',
    current_step INT DEFAULT 1,
    total_steps INT DEFAULT 0,
    duration_ms INT DEFAULT 0 COMMENT '累计停留时长',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户烹饪会话表';

-- 每日推荐结果表
CREATE TABLE IF NOT EXISTS daily_recipe_recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    biz_date DATE NOT NULL COMMENT '业务日期',
    recipe_id INT NOT NULL,
    rank_no INT NOT NULL COMMENT '当日排序位次',
    selected_for_delivery TINYINT DEFAULT 0 COMMENT '是否入选当日固定16条',
    model_score DECIMAL(16,8) DEFAULT 0 COMMENT '模型分数',
    reason_json JSON NULL COMMENT '推荐理由JSON',
    model_version VARCHAR(64) NOT NULL COMMENT '模型版本',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_daily_reco_user_date_rank (user_id, biz_date, rank_no),
    UNIQUE KEY uk_daily_reco_user_date_recipe (user_id, biz_date, recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐结果表';

-- 每日推荐任务执行记录表
CREATE TABLE IF NOT EXISTS daily_recommend_job_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_date DATE NOT NULL COMMENT '任务业务日期',
    phase VARCHAR(32) NOT NULL COMMENT '任务阶段',
    model_version VARCHAR(64) NOT NULL COMMENT '模型版本',
    affected_users INT DEFAULT 0 COMMENT '影响用户数',
    affected_recipes INT DEFAULT 0 COMMENT '影响食谱数',
    status VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT '任务状态',
    error_message TEXT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    KEY idx_daily_job_runs_date_phase (job_date, phase, status),
    KEY idx_daily_job_runs_started (started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐任务执行记录表';

-- ===================== ID映射表 =====================

-- 用户ID映射表
CREATE TABLE IF NOT EXISTS user_id_mapping (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_user_id INT NOT NULL,
    new_user_id INT NOT NULL,
    UNIQUE KEY uk_old_user_id (old_user_id),
    FOREIGN KEY (new_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户ID映射表';

-- 菜谱ID映射表
CREATE TABLE IF NOT EXISTS recipe_id_mapping (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_recipe_id INT NOT NULL,
    new_recipe_id INT NOT NULL,
    UNIQUE KEY uk_old_recipe_id (old_recipe_id),
    FOREIGN KEY (new_recipe_id) REFERENCES recipes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱ID映射表';

-- 创建索引优化查询
CREATE INDEX idx_recipe_taste ON recipes(taste_id);
CREATE INDEX idx_recipe_technique ON recipes(technique_id);
CREATE INDEX idx_recipe_difficulty ON recipes(difficulty_id);
CREATE INDEX idx_recipe_like_count ON recipes(like_count DESC);
CREATE INDEX idx_recipe_create_time ON recipes(create_time DESC);
CREATE INDEX idx_recipes_status_create_time ON recipes(status, create_time DESC, id DESC);
CREATE INDEX idx_recipes_status_like_count ON recipes(status, like_count DESC, id DESC);
CREATE INDEX idx_recipes_status_favorite_count ON recipes(status, favorite_count DESC, id DESC);
CREATE INDEX idx_recipes_status_rating_count ON recipes(status, rating_count DESC, id DESC);
CREATE INDEX idx_recipes_admin_create_time ON recipes(create_time DESC, id DESC);
CREATE INDEX idx_comment_recipe ON comments(recipe_id);
CREATE INDEX idx_comment_recipe_publish_time ON comments(recipe_id, publish_time DESC, id DESC);
CREATE INDEX idx_comment_create_time ON comments(create_time DESC);
CREATE INDEX idx_comment_user ON comments(user_id);
CREATE INDEX idx_comment_user_create ON comments(user_id, create_time);
CREATE INDEX idx_comment_likes_comment_time ON comment_likes(comment_id, create_time DESC);
CREATE INDEX idx_comment_likes_user_time ON comment_likes(user_id, create_time DESC);
CREATE INDEX idx_interaction_user ON interactions(user_id);
CREATE INDEX idx_interaction_recipe ON interactions(recipe_id);
CREATE INDEX idx_interaction_user_type ON interactions(user_id, interaction_type);
CREATE INDEX idx_interactions_create_time_type ON interactions(create_time DESC, interaction_type);
CREATE INDEX idx_user_create_time ON users(create_time DESC);
CREATE INDEX idx_users_status_create_time ON users(status, create_time DESC, id DESC);
CREATE INDEX idx_user_pref_user ON user_preference_profiles(user_id);
CREATE INDEX idx_behavior_user_time ON behavior_events(user_id, create_time DESC, id DESC, recipe_id);
CREATE INDEX idx_behavior_session_time ON behavior_events(session_id, create_time DESC);
CREATE INDEX idx_behavior_event_time ON behavior_events(event_type, create_time DESC);
CREATE INDEX idx_behavior_recipe_time ON behavior_events(recipe_id, create_time DESC);
CREATE INDEX idx_behavior_user_event_time_recipe ON behavior_events(user_id, event_type, create_time DESC, id DESC, recipe_id);
CREATE INDEX idx_behavior_user_time_scene ON behavior_events(user_id, create_time DESC, scene_code);
CREATE INDEX idx_cook_session_user_status_time ON cooking_sessions(user_id, status, last_active_time DESC);
CREATE INDEX idx_cook_session_recipe_time ON cooking_sessions(recipe_id, started_at DESC);
CREATE INDEX idx_cook_session_user_start_time ON cooking_sessions(user_id, started_at DESC);
CREATE INDEX idx_daily_reco_user_date_delivery ON daily_recipe_recommendations(user_id, biz_date, selected_for_delivery, rank_no);
CREATE INDEX idx_daily_reco_date_version ON daily_recipe_recommendations(biz_date, model_version);

-- 菜谱 - 分类关联表索引（优化菜谱大全查询）
CREATE INDEX idx_recipe_categories_composite ON recipe_categories(category_id, recipe_id);
CREATE INDEX idx_recipe_categories_recipe ON recipe_categories(recipe_id, category_id);

-- 菜谱 - 食材关联表索引（优化相关推荐查询）
CREATE INDEX idx_ingredient_recipe_type ON recipe_ingredients(ingredient_type, recipe_id);
CREATE INDEX idx_ingredient_ingredient_id ON recipe_ingredients(ingredient_id);
CREATE INDEX idx_recipe_ingredients_composite ON recipe_ingredients(recipe_id, ingredient_type, ingredient_id);

-- ===================== 管理后台表 =====================

-- 管理员表
CREATE TABLE IF NOT EXISTS admins (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'admin',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    admin_id INT,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id INT,
    detail TEXT,
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ===================== 统计汇总表 =====================

-- 1. 统计概览汇总表（实时）
CREATE TABLE IF NOT EXISTS `statistics_overview` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_users` INT DEFAULT 0 COMMENT '总用户数',
    `total_recipes` INT DEFAULT 0 COMMENT '总菜谱数',
    `total_categories` INT DEFAULT 0 COMMENT '总分类数',
    `total_comments` INT DEFAULT 0 COMMENT '总评论数',
    `today_views` INT DEFAULT 0 COMMENT '今日浏览数',
    `today_new_users` INT DEFAULT 0 COMMENT '今日新增用户',
    `today_new_recipes` INT DEFAULT 0 COMMENT '今日新增菜谱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计概览汇总表';

-- 2. 用户趋势表（按天聚合）
CREATE TABLE IF NOT EXISTS `user_trend_daily` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `new_users_count` INT DEFAULT 0 COMMENT '新增用户数',
    `total_users` INT DEFAULT 0 COMMENT '累计用户数',
    `active_users` INT DEFAULT 0 COMMENT '活跃用户数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户趋势日报表';

-- 3. 菜谱趋势表（按天聚合）
CREATE TABLE IF NOT EXISTS `recipe_trend_daily` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `new_recipes_count` INT DEFAULT 0 COMMENT '新增菜谱数',
    `total_recipes` INT DEFAULT 0 COMMENT '累计菜谱数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱趋势日报表';

-- 4. 评论趋势表（按天聚合）
CREATE TABLE IF NOT EXISTS `comment_trend_daily` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `new_comments_count` INT DEFAULT 0 COMMENT '新增评论数',
    `total_comments` INT DEFAULT 0 COMMENT '累计评论数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论趋势日报表';

-- 5. 分类分布汇总表（实时）
CREATE TABLE IF NOT EXISTS `category_distribution` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `category_id` INT NOT NULL COMMENT '分类 ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `recipe_count` INT DEFAULT 0 COMMENT '菜谱数量',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_category_date` (`category_id`, `stat_date`),
    KEY `idx_recipe_count` (`recipe_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类分布汇总表';

-- 6. 难度分布汇总表（实时）
CREATE TABLE IF NOT EXISTS `difficulty_distribution` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `difficulty_id` INT NOT NULL COMMENT '难度 ID',
    `difficulty_name` VARCHAR(50) NOT NULL COMMENT '难度名称',
    `recipe_count` INT DEFAULT 0 COMMENT '菜谱数量',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_difficulty_date` (`difficulty_id`, `stat_date`),
    KEY `idx_recipe_count` (`recipe_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='难度分布汇总表';

-- 7. 热门菜谱汇总表（Top 10，每小时更新）
CREATE TABLE IF NOT EXISTS `top_recipes_hourly` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `recipe_id` INT NOT NULL COMMENT '菜谱 ID',
    `recipe_title` VARCHAR(255) NOT NULL COMMENT '菜谱标题',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `view_count` INT DEFAULT 0 COMMENT '浏览数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `rank` INT NOT NULL COMMENT '排名',
    `stat_hour` DATETIME NOT NULL COMMENT '统计小时',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_recipe_hour` (`recipe_id`, `stat_hour`),
    KEY `idx_rank` (`rank`),
    KEY `idx_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门菜谱小时榜';

-- 8. 热门评论菜谱汇总表（Top 10，每天更新）
CREATE TABLE IF NOT EXISTS `top_commented_recipes_daily` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `recipe_id` INT NOT NULL COMMENT '菜谱 ID',
    `recipe_title` VARCHAR(255) NOT NULL COMMENT '菜谱标题',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `rank` INT NOT NULL COMMENT '排名',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_recipe_date` (`recipe_id`, `stat_date`),
    KEY `idx_rank` (`rank`),
    KEY `idx_comment_count` (`comment_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门评论菜谱日报表';

-- 9. 互动统计表（按天聚合）
CREATE TABLE IF NOT EXISTS `interaction_daily` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_interactions` INT DEFAULT 0 COMMENT '总互动数',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `view_count` INT DEFAULT 0 COMMENT '浏览数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_date` (`stat_date`),
    KEY `idx_total_interactions` (`total_interactions`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动统计日报表';

-- 初始化统计概览
INSERT IGNORE INTO statistics_overview 
(stat_date, total_users, total_recipes, total_categories, total_comments, today_views, today_new_users, today_new_recipes)
SELECT 
    CURDATE() as stat_date,
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM recipes) as total_recipes,
    (SELECT COUNT(*) FROM categories) as total_categories,
    (SELECT COUNT(*) FROM comments) as total_comments,
    (SELECT COUNT(*) FROM interactions WHERE create_time >= CURDATE()) as today_views,
    (SELECT COUNT(*) FROM users WHERE create_time >= CURDATE()) as today_new_users,
    (SELECT COUNT(*) FROM recipes WHERE create_time >= CURDATE()) as today_new_recipes;

-- 初始化分类分布
INSERT IGNORE INTO category_distribution (category_id, category_name, recipe_count, stat_date)
SELECT 
    c.id as category_id,
    c.name as category_name,
    COUNT(rc.recipe_id) as recipe_count,
    CURDATE() as stat_date
FROM categories c
LEFT JOIN recipe_categories rc ON c.id = rc.category_id
GROUP BY c.id, c.name;

-- 初始化难度分布
INSERT IGNORE INTO difficulty_distribution (difficulty_id, difficulty_name, recipe_count, stat_date)
SELECT 
    d.id as difficulty_id,
    d.name as difficulty_name,
    COUNT(r.id) as recipe_count,
    CURDATE() as stat_date
FROM difficulties d
LEFT JOIN recipes r ON d.id = r.difficulty_id
GROUP BY d.id, d.name;

DELIMITER $$

-- 用户新增触发器
CREATE TRIGGER IF NOT EXISTS trg_user_insert AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- 更新统计概览
    INSERT INTO statistics_overview (stat_date, total_users, today_new_users)
    VALUES (CURDATE(), (SELECT COUNT(*) FROM users), 1)
    ON DUPLICATE KEY UPDATE
        total_users = total_users + 1,
        today_new_users = today_new_users + 1,
        update_time = NOW();
    
    -- 更新用户趋势
    INSERT INTO user_trend_daily (stat_date, new_users_count, total_users)
    VALUES (CURDATE(), 1, (SELECT COUNT(*) FROM users))
    ON DUPLICATE KEY UPDATE
        new_users_count = new_users_count + 1,
        total_users = VALUES(total_users);
END$$

-- 菜谱新增触发器
CREATE TRIGGER IF NOT EXISTS trg_recipe_insert AFTER INSERT ON recipes
FOR EACH ROW
BEGIN
    -- 更新统计概览
    INSERT INTO statistics_overview (stat_date, total_recipes, today_new_recipes)
    VALUES (CURDATE(), (SELECT COUNT(*) FROM recipes), 1)
    ON DUPLICATE KEY UPDATE
        total_recipes = total_recipes + 1,
        today_new_recipes = today_new_recipes + 1,
        update_time = NOW();
    
    -- 更新菜谱趋势
    INSERT INTO recipe_trend_daily (stat_date, new_recipes_count, total_recipes)
    VALUES (CURDATE(), 1, (SELECT COUNT(*) FROM recipes))
    ON DUPLICATE KEY UPDATE
        new_recipes_count = new_recipes_count + 1,
        total_recipes = VALUES(total_recipes);
END$$

-- 评论新增触发器
CREATE TRIGGER IF NOT EXISTS trg_comment_insert AFTER INSERT ON comments
FOR EACH ROW
BEGIN
    -- 更新统计概览
    INSERT INTO statistics_overview (stat_date, total_comments)
    VALUES (CURDATE(), 1)
    ON DUPLICATE KEY UPDATE
        total_comments = total_comments + 1;
    
    -- 更新评论趋势
    INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments)
    VALUES (CURDATE(), 1, 1)
    ON DUPLICATE KEY UPDATE
        new_comments_count = new_comments_count + 1,
        total_comments = total_comments + 1;
END$$

-- 互动新增触发器
CREATE TRIGGER IF NOT EXISTS trg_interaction_insert AFTER INSERT ON interactions
FOR EACH ROW
BEGIN
    -- 更新统计概览
    INSERT INTO statistics_overview (stat_date, today_views)
    VALUES (CURDATE(), (SELECT COUNT(*) FROM interactions WHERE create_time >= CURDATE()))
    ON DUPLICATE KEY UPDATE
        today_views = today_views + 1,
        update_time = NOW();
    
    -- 更新互动统计
    INSERT INTO interaction_daily (stat_date, total_interactions, favorite_count, view_count)
    VALUES (
        CURDATE(), 
        1,
        IF(NEW.interaction_type = 'favorite', 1, 0),
        IF(NEW.interaction_type != 'favorite', 1, 0)
    )
    ON DUPLICATE KEY UPDATE
        total_interactions = total_interactions + 1,
        favorite_count = favorite_count + IF(NEW.interaction_type = 'favorite', 1, 0),
        view_count = view_count + IF(NEW.interaction_type != 'favorite', 1, 0);
END$$

DELIMITER ;
