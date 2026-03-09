-- Statistics Summary Tables for Dashboard Performance Optimization

-- 1. Statistics Overview Table
CREATE TABLE IF NOT EXISTS statistics_overview (
    stat_date DATE PRIMARY KEY,
    total_users INT DEFAULT 0,
    total_recipes INT DEFAULT 0,
    total_categories INT DEFAULT 0,
    total_comments INT DEFAULT 0,
    today_views INT DEFAULT 0,
    today_new_users INT DEFAULT 0,
    today_new_recipes INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. User Trend Daily Table
CREATE TABLE IF NOT EXISTS user_trend_daily (
    stat_date DATE PRIMARY KEY,
    new_users_count INT DEFAULT 0,
    total_users INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Recipe Trend Daily Table
CREATE TABLE IF NOT EXISTS recipe_trend_daily (
    stat_date DATE PRIMARY KEY,
    new_recipes_count INT DEFAULT 0,
    total_recipes INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Comment Trend Daily Table
CREATE TABLE IF NOT EXISTS comment_trend_daily (
    stat_date DATE PRIMARY KEY,
    new_comments_count INT DEFAULT 0,
    total_comments INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Category Distribution Summary Table
CREATE TABLE IF NOT EXISTS category_distribution_summary (
    stat_date DATE NOT NULL,
    category_id INT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    recipe_count INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stat_date, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Difficulty Distribution Summary Table
CREATE TABLE IF NOT EXISTS difficulty_distribution_summary (
    stat_date DATE NOT NULL,
    difficulty_id INT NOT NULL,
    difficulty_name VARCHAR(50) NOT NULL,
    recipe_count INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stat_date, difficulty_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Top Recipes Hourly Table
CREATE TABLE IF NOT EXISTS top_recipes_hourly (
    stat_time DATETIME NOT NULL,
    recipe_id INT NOT NULL,
    recipe_title VARCHAR(200) NOT NULL,
    like_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    recipe_rank INT NOT NULL,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stat_time, recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Top Commented Recipes Hourly Table
CREATE TABLE IF NOT EXISTS top_commented_recipes_hourly (
    stat_time DATETIME NOT NULL,
    recipe_id INT NOT NULL,
    recipe_title VARCHAR(200) NOT NULL,
    comment_count INT DEFAULT 0,
    recipe_rank INT NOT NULL,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stat_time, recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initialize statistics overview
INSERT INTO statistics_overview (
    stat_date, total_users, total_recipes, total_categories, total_comments,
    today_views, today_new_users, today_new_recipes
)
SELECT 
    CURDATE() as stat_date,
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM recipes) as total_recipes,
    (SELECT COUNT(*) FROM categories) as total_categories,
    (SELECT COUNT(*) FROM comments) as total_comments,
    (SELECT COUNT(*) FROM interactions WHERE create_time >= CURDATE()) as today_views,
    (SELECT COUNT(*) FROM users WHERE create_time >= CURDATE()) as today_new_users,
    (SELECT COUNT(*) FROM recipes WHERE create_time >= CURDATE()) as today_new_recipes
ON DUPLICATE KEY UPDATE
    total_users = VALUES(total_users),
    total_recipes = VALUES(total_recipes),
    total_categories = VALUES(total_categories),
    total_comments = VALUES(total_comments),
    today_views = VALUES(today_views),
    today_new_users = VALUES(today_new_users),
    today_new_recipes = VALUES(today_new_recipes);

-- Initialize user trend (last 7 days)
INSERT INTO user_trend_daily (stat_date, new_users_count, total_users)
SELECT 
    DATE(create_time) as stat_date,
    COUNT(*) as new_users_count,
    (SELECT COUNT(*) FROM users WHERE create_time <= DATE_ADD(DATE(create_time), INTERVAL 1 DAY)) as total_users
FROM users
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE
    new_users_count = VALUES(new_users_count),
    total_users = VALUES(total_users);

-- Initialize recipe trend (last 7 days)
INSERT INTO recipe_trend_daily (stat_date, new_recipes_count, total_recipes)
SELECT 
    DATE(create_time) as stat_date,
    COUNT(*) as new_recipes_count,
    (SELECT COUNT(*) FROM recipes WHERE create_time <= DATE_ADD(DATE(create_time), INTERVAL 1 DAY)) as total_recipes
FROM recipes
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE
    new_recipes_count = VALUES(new_recipes_count),
    total_recipes = VALUES(total_recipes);

-- Initialize comment trend (last 7 days)
INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments)
SELECT 
    DATE(create_time) as stat_date,
    COUNT(*) as new_comments_count,
    (SELECT COUNT(*) FROM comments WHERE create_time <= DATE_ADD(DATE(create_time), INTERVAL 1 DAY)) as total_comments
FROM comments
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE
    new_comments_count = VALUES(new_comments_count),
    total_comments = VALUES(total_comments);

-- Initialize category distribution
INSERT INTO category_distribution_summary (stat_date, category_id, category_name, recipe_count)
SELECT 
    CURDATE() as stat_date,
    c.id as category_id,
    c.name as category_name,
    COUNT(rc.recipe_id) as recipe_count
FROM categories c
LEFT JOIN recipe_categories rc ON c.id = rc.category_id
GROUP BY c.id, c.name
ON DUPLICATE KEY UPDATE
    recipe_count = VALUES(recipe_count);

-- Initialize difficulty distribution
INSERT INTO difficulty_distribution_summary (stat_date, difficulty_id, difficulty_name, recipe_count)
SELECT 
    CURDATE() as stat_date,
    d.id as difficulty_id,
    d.name as difficulty_name,
    COUNT(r.id) as recipe_count
FROM difficulties d
LEFT JOIN recipes r ON d.id = r.difficulty_id
GROUP BY d.id, d.name
ON DUPLICATE KEY UPDATE
    recipe_count = VALUES(recipe_count);

-- Initialize top recipes
INSERT INTO top_recipes_hourly (stat_time, recipe_id, recipe_title, like_count, view_count, comment_count, recipe_rank)
SELECT 
    NOW() as stat_time,
    r.id as recipe_id,
    r.title as recipe_title,
    r.like_count as like_count,
    r.rating_count as view_count,
    r.reply_count as comment_count,
    (@rank := @rank + 1) as recipe_rank
FROM recipes r, (SELECT @rank := 0) r0
ORDER BY r.like_count DESC
LIMIT 10
ON DUPLICATE KEY UPDATE
    like_count = VALUES(like_count),
    view_count = VALUES(view_count),
    comment_count = VALUES(comment_count),
    recipe_rank = VALUES(recipe_rank);

-- Initialize top commented recipes
INSERT INTO top_commented_recipes_hourly (stat_time, recipe_id, recipe_title, comment_count, recipe_rank)
SELECT 
    NOW() as stat_time,
    r.id as recipe_id,
    r.title as recipe_title,
    COUNT(c.id) as comment_count,
    (@rank := @rank + 1) as recipe_rank
FROM recipes r
LEFT JOIN comments c ON r.id = c.recipe_id
CROSS JOIN (SELECT @rank := 0) r0
GROUP BY r.id, r.title
ORDER BY comment_count DESC
LIMIT 10
ON DUPLICATE KEY UPDATE
    comment_count = VALUES(comment_count),
    recipe_rank = VALUES(recipe_rank);

-- Create indexes
CREATE INDEX idx_cat_dist_date ON category_distribution_summary(stat_date);
CREATE INDEX idx_cat_dist_category ON category_distribution_summary(category_id);
CREATE INDEX idx_dif_dist_date ON difficulty_distribution_summary(stat_date);
CREATE INDEX idx_top_recipes_time ON top_recipes_hourly(stat_time);
CREATE INDEX idx_top_recipes_rank ON top_recipes_hourly(recipe_rank);
CREATE INDEX idx_top_commented_time ON top_commented_recipes_hourly(stat_time);
CREATE INDEX idx_top_commented_rank ON top_commented_recipes_hourly(recipe_rank);
