-- foodrec 数据库增量补丁（2026-03-20）
-- 适用场景：先导入较旧的完整 food_recommend 备份，再补齐当前版本依赖的结构/索引/触发器
-- 手工导入命令：
-- mysql -uroot -p food_recommend < backend/let-me-cook/src/main/resources/db/migration/V6__daily_recommendation_comment_patch.sql

USE food_recommend;

DROP PROCEDURE IF EXISTS sp_apply_foodrec_v6_patch;
DELIMITER $$
CREATE PROCEDURE sp_apply_foodrec_v6_patch()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE categories ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ingredients' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE ingredients ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tastes' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE tastes ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'techniques' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE techniques ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'time_costs' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE time_costs ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'difficulties' AND COLUMN_NAME = 'recipe_count'
    ) THEN
        ALTER TABLE difficulties ADD COLUMN recipe_count INT DEFAULT 0 COMMENT '关联食谱数量';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND INDEX_NAME = 'idx_categories_recipe_count'
    ) THEN
        CREATE INDEX idx_categories_recipe_count ON categories(recipe_count);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ingredients' AND INDEX_NAME = 'idx_ingredients_recipe_count'
    ) THEN
        CREATE INDEX idx_ingredients_recipe_count ON ingredients(recipe_count);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tastes' AND INDEX_NAME = 'idx_tastes_recipe_count'
    ) THEN
        CREATE INDEX idx_tastes_recipe_count ON tastes(recipe_count);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'techniques' AND INDEX_NAME = 'idx_techniques_recipe_count'
    ) THEN
        CREATE INDEX idx_techniques_recipe_count ON techniques(recipe_count);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'time_costs' AND INDEX_NAME = 'idx_time_costs_recipe_count'
    ) THEN
        CREATE INDEX idx_time_costs_recipe_count ON time_costs(recipe_count);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'difficulties' AND INDEX_NAME = 'idx_difficulties_recipe_count'
    ) THEN
        CREATE INDEX idx_difficulties_recipe_count ON difficulties(recipe_count);
    END IF;

    UPDATE categories c
    SET recipe_count = COALESCE((
        SELECT COUNT(DISTINCT rc.recipe_id)
        FROM recipe_categories rc
        WHERE rc.category_id = c.id
    ), 0);

    UPDATE ingredients i
    SET recipe_count = COALESCE((
        SELECT COUNT(DISTINCT ri.recipe_id)
        FROM recipe_ingredients ri
        WHERE ri.ingredient_id = i.id
    ), 0);

    UPDATE tastes t
    SET recipe_count = COALESCE((
        SELECT COUNT(*)
        FROM recipes r
        WHERE r.taste_id = t.id
    ), 0);

    UPDATE techniques t
    SET recipe_count = COALESCE((
        SELECT COUNT(*)
        FROM recipes r
        WHERE r.technique_id = t.id
    ), 0);

    UPDATE time_costs tc
    SET recipe_count = COALESCE((
        SELECT COUNT(*)
        FROM recipes r
        WHERE r.time_cost_id = tc.id
    ), 0);

    UPDATE difficulties d
    SET recipe_count = COALESCE((
        SELECT COUNT(*)
        FROM recipes r
        WHERE r.difficulty_id = d.id
    ), 0);

    CREATE TABLE IF NOT EXISTS daily_recipe_recommendations (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id INT NOT NULL,
        biz_date DATE NOT NULL COMMENT '业务日期',
        recipe_id INT NOT NULL,
        rank_no INT NOT NULL COMMENT '排序位次',
        selected_for_delivery TINYINT DEFAULT 0 COMMENT '是否入选展示池',
        model_score DECIMAL(16,8) DEFAULT 0 COMMENT '模型分数',
        reason_json JSON NULL COMMENT '推荐理由 JSON',
        model_version VARCHAR(64) NOT NULL COMMENT '模型版本',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_daily_reco_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_daily_reco_recipe FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
        UNIQUE KEY uk_daily_reco_user_date_rank (user_id, biz_date, rank_no),
        UNIQUE KEY uk_daily_reco_user_date_recipe (user_id, biz_date, recipe_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐结果表';

    CREATE TABLE IF NOT EXISTS daily_recommend_job_runs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        job_date DATE NOT NULL COMMENT '任务业务日期',
        phase VARCHAR(32) NOT NULL COMMENT '任务阶段',
        model_version VARCHAR(64) NOT NULL COMMENT '模型版本',
        affected_users INT DEFAULT 0 COMMENT '影响用户数',
        affected_recipes INT DEFAULT 0 COMMENT '影响食谱数',
        status VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT '状态',
        error_message TEXT NULL COMMENT '错误信息',
        started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        finished_at DATETIME NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐任务执行记录表';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_recipe_recommendations' AND INDEX_NAME = 'idx_daily_reco_user_date_delivery'
    ) THEN
        CREATE INDEX idx_daily_reco_user_date_delivery
            ON daily_recipe_recommendations(user_id, biz_date, selected_for_delivery, rank_no);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_recipe_recommendations' AND INDEX_NAME = 'idx_daily_reco_date_version'
    ) THEN
        CREATE INDEX idx_daily_reco_date_version
            ON daily_recipe_recommendations(biz_date, model_version);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_recommend_job_runs' AND INDEX_NAME = 'idx_daily_job_runs_date_phase'
    ) THEN
        CREATE INDEX idx_daily_job_runs_date_phase
            ON daily_recommend_job_runs(job_date, phase, status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_recommend_job_runs' AND INDEX_NAME = 'idx_daily_job_runs_started'
    ) THEN
        CREATE INDEX idx_daily_job_runs_started
            ON daily_recommend_job_runs(started_at DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND INDEX_NAME = 'idx_comment_recipe_publish_time'
    ) THEN
        CREATE INDEX idx_comment_recipe_publish_time
            ON comments(recipe_id, publish_time DESC, id DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND INDEX_NAME = 'idx_comment_create_time'
    ) THEN
        CREATE INDEX idx_comment_create_time
            ON comments(create_time DESC);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND INDEX_NAME = 'idx_comment_user_create'
    ) THEN
        CREATE INDEX idx_comment_user_create
            ON comments(user_id, create_time);
    END IF;
END$$
DELIMITER ;

CALL sp_apply_foodrec_v6_patch();
DROP PROCEDURE IF EXISTS sp_apply_foodrec_v6_patch;

DROP TRIGGER IF EXISTS trg_comment_insert;
DELIMITER $$
CREATE TRIGGER trg_comment_insert
AFTER INSERT ON comments
FOR EACH ROW
BEGIN
    INSERT INTO statistics_overview (stat_date, total_comments)
    VALUES (CURDATE(), 1)
    ON DUPLICATE KEY UPDATE
        total_comments = total_comments + 1;

    INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments)
    VALUES (CURDATE(), 1, 1)
    ON DUPLICATE KEY UPDATE
        new_comments_count = new_comments_count + 1,
        total_comments = total_comments + 1;
END$$
DELIMITER ;
