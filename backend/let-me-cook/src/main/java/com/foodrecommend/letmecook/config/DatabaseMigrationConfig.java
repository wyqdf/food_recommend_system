package com.foodrecommend.letmecook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseMigrationConfig {

    private final JdbcTemplate jdbcTemplate;
    private volatile String currentSchema;

    @Value("${database.migration.analyze-on-startup:false}")
    private boolean analyzeOnStartup;

    @Bean
    public CommandLineRunner addPerformanceIndexes() {
        return args -> {
            log.info("开始执行数据库迁移和性能优化...");
            
            addCookwaresTable();
            addRecipeCountColumns();
            enhanceOperationLogsTable();
            addUserPreferenceProfilesTable();
            addBehaviorEventsTable();
            addCookingSessionsTable();
            addDailyRecommendationTables();
            addCommentLikesTable();
            refreshCommentInsertTrigger();
            
            log.info("开始执行数据库性能优化索引...");
            addIndexIfNotExists("idx_comment_recipe_publish_time", "comments", "recipe_id, publish_time DESC, id DESC");
            addIndexIfNotExists("idx_comment_create_time", "comments", "create_time DESC");
            addIndexIfNotExists("idx_comment_user_create", "comments", "user_id, create_time");
            addIndexIfNotExists("idx_comment_likes_comment_time", "comment_likes", "comment_id, create_time DESC");
            addIndexIfNotExists("idx_comment_likes_user_time", "comment_likes", "user_id, create_time DESC");
            addIndexIfNotExists("idx_recipes_status_create_time", "recipes", "status, create_time DESC, id DESC");
            addIndexIfNotExists("idx_recipes_status_like_count", "recipes", "status, like_count DESC, id DESC");
            addIndexIfNotExists("idx_recipes_status_favorite_count", "recipes", "status, favorite_count DESC, id DESC");
            addIndexIfNotExists("idx_recipes_status_rating_count", "recipes", "status, rating_count DESC, id DESC");
            addIndexIfNotExists("idx_recipes_status_id", "recipes", "status, id");
            addIndexIfNotExists("idx_recipes_admin_create_time", "recipes", "create_time DESC, id DESC");
            addIndexIfNotExists("idx_interaction_user_type", "interactions", "user_id, interaction_type");
            addIndexIfNotExists("idx_interactions_user_recipe_type", "interactions", "user_id, recipe_id, interaction_type");
            addIndexIfNotExists("idx_interactions_user_type_time_recipe", "interactions", "user_id, interaction_type, create_time DESC, id DESC, recipe_id");
            addIndexIfNotExists("idx_interactions_create_time_type", "interactions", "create_time DESC, interaction_type");
            addIndexIfNotExists("idx_user_create_time", "users", "create_time DESC");
            addIndexIfNotExists("idx_users_status_create_time", "users", "status, create_time DESC, id DESC");
            addIndexIfNotExists("idx_recipe_author_uid", "recipes", "author_uid");
            addIndexIfNotExists("idx_recipe_ingredients_ing_type_recipe", "recipe_ingredients", "ingredient_id, ingredient_type, recipe_id");
            addIndexIfNotExists("idx_behavior_user_time", "behavior_events", "user_id, create_time DESC, id DESC, recipe_id");
            addIndexIfNotExists("idx_behavior_event_time", "behavior_events", "event_type, create_time DESC");
            addIndexIfNotExists("idx_behavior_recipe_time", "behavior_events", "recipe_id, create_time DESC");
            addIndexIfNotExists("idx_behavior_session_time", "behavior_events", "session_id, create_time DESC");
            addIndexIfNotExists("idx_behavior_user_event_time_recipe", "behavior_events", "user_id, event_type, create_time DESC, id DESC, recipe_id");
            addIndexIfNotExists("idx_behavior_user_time_scene", "behavior_events", "user_id, create_time DESC, scene_code");
            addIndexIfNotExists("idx_cook_session_user_status_time", "cooking_sessions", "user_id, status, last_active_time DESC");
            addIndexIfNotExists("idx_cook_session_recipe_time", "cooking_sessions", "recipe_id, started_at DESC");
            addIndexIfNotExists("idx_cook_session_user_start_time", "cooking_sessions", "user_id, started_at DESC");
            enforceFavoriteUniqueness();
            syncRecipeFavoriteCounts();
            setIndexInvisibleIfExists("comments", "idx_comment_recipe");
            setIndexInvisibleIfExists("recipes", "idx_createtime");
            setIndexInvisibleIfExists("recipes", "idx_like_count");
            setIndexInvisibleIfExists("users", "idx_users_status");
            setIndexInvisibleIfExists("users", "idx_user_status");
            setIndexInvisibleIfExists("recipe_categories", "idx_recipe_categories_recipe");
            setIndexInvisibleIfExists("recipe_ingredients", "idx_ri");

            if (analyzeOnStartup) {
                analyzeTables();
            } else {
                log.info("跳过启动时 ANALYZE TABLE，可通过 database.migration.analyze-on-startup=true 开启");
            }

            log.info("数据库迁移和性能优化执行完成");
        };
    }

    private void refreshCommentInsertTrigger() {
        try {
            log.info("刷新评论插入触发器，避免整表 COUNT(*) 拖慢写入...");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_comment_insert");
            jdbcTemplate.execute("""
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
                    END
                    """);
            log.info("评论插入触发器刷新完成");
        } catch (Exception e) {
            log.warn("刷新评论插入触发器失败：{}", e.getMessage());
        }
    }

    private void syncRecipeFavoriteCounts() {
        try {
            log.info("同步 recipes.favorite_count 与 interactions 收藏数据...");
            int updated = jdbcTemplate.update("""
                    UPDATE recipes r
                    LEFT JOIN (
                        SELECT recipe_id, COUNT(*) AS favorite_count
                        FROM interactions
                        WHERE interaction_type = 'favorite'
                        GROUP BY recipe_id
                    ) fav ON fav.recipe_id = r.id
                    SET r.favorite_count = COALESCE(fav.favorite_count, 0)
                    WHERE COALESCE(r.favorite_count, -1) <> COALESCE(fav.favorite_count, 0)
                    """);
            log.info("recipes.favorite_count 同步完成，更新 {} 条记录", updated);
        } catch (Exception e) {
            log.warn("同步 recipes.favorite_count 失败：{}", e.getMessage());
        }
    }

    private void enforceFavoriteUniqueness() {
        try {
            log.info("收敛 interactions 中的收藏唯一性约束...");
            int deleted = jdbcTemplate.update("""
                    DELETE i1
                    FROM interactions i1
                    INNER JOIN interactions i2
                        ON i1.user_id = i2.user_id
                        AND i1.recipe_id = i2.recipe_id
                        AND i1.interaction_type = 'favorite'
                        AND i2.interaction_type = 'favorite'
                        AND i1.id > i2.id
                    """);
            if (deleted > 0) {
                log.info("已清理重复 favorite 记录 {} 条", deleted);
            }

            addFavoriteUniqueFlagColumnIfNotExists();
            addUniqueIndexIfNotExists(
                    "uk_interactions_user_recipe_favorite",
                    "interactions",
                    "user_id, recipe_id, favorite_unique_flag");
        } catch (Exception e) {
            log.warn("收敛收藏唯一性失败：{}", e.getMessage());
        }
    }

    private void addFavoriteUniqueFlagColumnIfNotExists() {
        try {
            if (checkColumnExists("interactions", "favorite_unique_flag")) {
                log.info("interactions.favorite_unique_flag 已存在，跳过");
                return;
            }
            log.info("为 interactions 添加 favorite_unique_flag 生成列...");
            jdbcTemplate.execute("""
                    ALTER TABLE interactions
                    ADD COLUMN favorite_unique_flag TINYINT
                    GENERATED ALWAYS AS (
                        CASE WHEN interaction_type = 'favorite' THEN 1 ELSE NULL END
                    ) STORED
                    """);
            log.info("favorite_unique_flag 生成列添加完成");
        } catch (Exception e) {
            log.warn("添加 favorite_unique_flag 生成列失败：{}", e.getMessage());
        }
    }
    
    private void addCookwaresTable() {
        try {
            log.info("检查并创建 cookwares 表...");
            boolean tableExists = checkTableExists("cookwares");
            if (!tableExists) {
                log.info("创建 cookwares 表...");
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS cookwares (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(50) NOT NULL UNIQUE,
                        recipe_count INT DEFAULT 0,
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厨具表'
                """);
                jdbcTemplate.execute("INSERT INTO cookwares (name) VALUES ('炒锅'), ('蒸锅'), ('砂锅'), ('烤箱'), ('微波炉'), ('电饭煲'), ('平底锅'), ('高压锅')");
                log.info("cookwares 表创建成功并插入初始数据");
            } else {
                log.info("cookwares 表已存在，跳过创建");
            }
        } catch (Exception e) {
            log.warn("创建 cookwares 表时出错：{}", e.getMessage());
        }
    }
    
    private boolean checkTableExists(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, getCurrentSchema(), tableName);
        return count != null && count > 0;
    }
    
    private void addRecipeCountColumns() {
        try {
            log.info("检查并添加 recipe_count 字段...");
            
            addColumnIfNotExists("tastes", "recipe_count", "INT DEFAULT 0");
            addColumnIfNotExists("techniques", "recipe_count", "INT DEFAULT 0");
            addColumnIfNotExists("time_costs", "recipe_count", "INT DEFAULT 0");
            addColumnIfNotExists("difficulties", "recipe_count", "INT DEFAULT 0");
            addColumnIfNotExists("ingredients", "recipe_count", "INT DEFAULT 0");
            addColumnIfNotExists("categories", "recipe_count", "INT DEFAULT 0");
            
            initializeRecipeCounts();
            
            log.info("recipe_count 字段迁移完成");
        } catch (Exception e) {
            log.warn("迁移 recipe_count 字段时出错：{}", e.getMessage());
        }
    }
    
    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try {
            boolean exists = checkColumnExists(tableName, columnName);
            if (!exists) {
                log.info("为表 {} 添加字段 {}", tableName, columnName);
                String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                jdbcTemplate.execute(sql);
                
                String indexSql = String.format("CREATE INDEX idx_%s_%s ON %s (%s)", tableName, columnName, tableName, columnName);
                try {
                    jdbcTemplate.execute(indexSql);
                } catch (Exception e) {
                    log.debug("创建索引时出错（可能已存在）：{}", e.getMessage());
                }
                
                log.info("字段 {} 添加成功", columnName);
            } else {
                log.info("表 {} 的字段 {} 已存在，跳过", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("添加字段 {}.{} 时出错：{}", tableName, columnName, e.getMessage());
        }
    }
    
    private boolean checkColumnExists(String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, getCurrentSchema(), tableName, columnName);
        return count != null && count > 0;
    }
    
    private void initializeRecipeCounts() {
        try {
            log.info("初始化各属性的食谱数量...");
            
            initializeRecipeCountForTable("tastes", "recipes", "taste_id");
            initializeRecipeCountForTable("techniques", "recipes", "technique_id");
            initializeRecipeCountForTable("time_costs", "recipes", "time_cost_id");
            initializeRecipeCountForTable("difficulties", "recipes", "difficulty_id");
            initializeRecipeCountForTable("ingredients", "recipe_ingredients", "ingredient_id", true);
            initializeRecipeCountForTable("categories", "recipe_categories", "category_id", true);
            
            log.info("食谱数量初始化完成");
        } catch (Exception e) {
            log.warn("初始化食谱数量时出错：{}", e.getMessage());
        }
    }
    
    private void initializeRecipeCountForTable(String targetTable, String sourceTable, String joinColumn) {
        initializeRecipeCountForTable(targetTable, sourceTable, joinColumn, false);
    }
    
    private void initializeRecipeCountForTable(String targetTable, String sourceTable, String joinColumn, boolean distinct) {
        try {
            String countFunc = distinct ? "COUNT(DISTINCT " + getPrimaryKeyColumn(sourceTable) + ")" : "COUNT(*)";
            String sql = String.format(
                "UPDATE %s t SET recipe_count = COALESCE((SELECT %s FROM %s s WHERE s.%s = t.id), 0) WHERE t.recipe_count = 0 OR t.recipe_count IS NULL",
                targetTable, countFunc, sourceTable, joinColumn
            );
            jdbcTemplate.execute(sql);
            log.info("更新 {} 的食谱数量完成", targetTable);
        } catch (Exception e) {
            log.warn("更新 {}.recipe_count 时出错：{}", targetTable, e.getMessage());
            try {
                String initSql = String.format("UPDATE %s SET recipe_count = 0 WHERE recipe_count IS NULL", targetTable);
                jdbcTemplate.execute(initSql);
            } catch (Exception ex) {
                log.warn("重置 {}.recipe_count 失败：{}", targetTable, ex.getMessage());
            }
        }
    }
    
    private String getPrimaryKeyColumn(String table) {
        switch (table) {
            case "recipe_ingredients": return "recipe_id";
            case "recipe_categories": return "recipe_id";
            default: return "id";
        }
    }

    private void addIndexIfNotExists(String indexName, String tableName, String columns) {
        try {
            boolean exists = checkIndexExists(tableName, indexName);
            if (!exists) {
                log.info("创建索引：{} on {} ({})", indexName, tableName, columns);
                String sql = String.format("CREATE INDEX %s ON %s (%s)", indexName, tableName, columns);
                jdbcTemplate.execute(sql);
                log.info("索引创建成功：{}", indexName);
            } else {
                log.info("索引已存在：{}", indexName);
            }
        } catch (Exception e) {
            log.warn("创建索引 {} 时出错：{}", indexName, e.getMessage());
        }
    }

    private void addUniqueIndexIfNotExists(String indexName, String tableName, String columns) {
        try {
            boolean exists = checkIndexExists(tableName, indexName);
            if (!exists) {
                log.info("创建唯一索引：{} on {} ({})", indexName, tableName, columns);
                String sql = String.format("CREATE UNIQUE INDEX %s ON %s (%s)", indexName, tableName, columns);
                jdbcTemplate.execute(sql);
                log.info("唯一索引创建成功：{}", indexName);
            } else {
                log.info("唯一索引已存在：{}", indexName);
            }
        } catch (Exception e) {
            log.warn("创建唯一索引 {} 时出错：{}", indexName, e.getMessage());
        }
    }

    private boolean checkIndexExists(String tableName, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND index_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, getCurrentSchema(), tableName, indexName);
        return count != null && count > 0;
    }

    private void setIndexInvisibleIfExists(String tableName, String indexName) {
        try {
            if (!checkIndexExists(tableName, indexName)) {
                log.info("索引不存在，跳过隐藏：{}.{}", tableName, indexName);
                return;
            }
            if (!isIndexVisible(tableName, indexName)) {
                log.info("索引已是 INVISIBLE：{}.{}", tableName, indexName);
                return;
            }
            log.info("隐藏冗余索引：{}.{}", tableName, indexName);
            jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER INDEX %s INVISIBLE", tableName, indexName));
            log.info("索引已隐藏：{}.{}", tableName, indexName);
        } catch (Exception e) {
            log.warn("隐藏索引 {}.{} 时出错：{}", tableName, indexName, e.getMessage());
        }
    }

    private boolean isIndexVisible(String tableName, String indexName) throws SQLException {
        String sql = "SELECT MAX(CASE WHEN IS_VISIBLE = 'YES' THEN 1 ELSE 0 END) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?";
        Integer visible = jdbcTemplate.queryForObject(sql, Integer.class, getCurrentSchema(), tableName, indexName);
        return visible != null && visible > 0;
    }

    private String getCurrentSchema() {
        String schema = currentSchema;
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        String resolvedSchema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (resolvedSchema == null || resolvedSchema.isBlank()) {
            throw new IllegalStateException("当前数据源未选择默认数据库，无法执行信息架构检查");
        }
        currentSchema = resolvedSchema;
        return resolvedSchema;
    }

    private void enhanceOperationLogsTable() {
        try {
            if (!checkTableExists("operation_logs")) {
                log.info("operation_logs 表不存在，跳过日志表增强");
                return;
            }

            addColumnIfNotExistsWithoutIndex("operation_logs", "user_agent", "VARCHAR(512) NULL");
            addIndexIfNotExists("idx_operation_logs_action", "operation_logs", "action");
            addIndexIfNotExists("idx_operation_logs_target_type", "operation_logs", "target_type");
            addIndexIfNotExists("idx_operation_logs_create_time", "operation_logs", "create_time DESC");
            addIndexIfNotExists("idx_operation_logs_admin_time", "operation_logs", "admin_id, create_time DESC");
        } catch (Exception e) {
            log.warn("增强 operation_logs 表失败：{}", e.getMessage());
        }
    }

    private void addColumnIfNotExistsWithoutIndex(String tableName, String columnName, String columnDefinition) {
        try {
            boolean exists = checkColumnExists(tableName, columnName);
            if (!exists) {
                log.info("为表 {} 添加字段 {}", tableName, columnName);
                String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                jdbcTemplate.execute(sql);
                log.info("字段 {} 添加成功", columnName);
            }
        } catch (Exception e) {
            log.warn("添加字段 {}.{} 时出错：{}", tableName, columnName, e.getMessage());
        }
    }

    private void addUserPreferenceProfilesTable() {
        try {
            if (checkTableExists("user_preference_profiles")) {
                log.info("user_preference_profiles 表已存在，跳过创建");
                return;
            }
            log.info("创建 user_preference_profiles 表...");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_preference_profiles (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL UNIQUE,
                        diet_goal VARCHAR(50),
                        cooking_skill VARCHAR(50),
                        time_budget VARCHAR(50),
                        preferred_tastes_json JSON NULL,
                        taboo_ingredients_json JSON NULL,
                        available_cookwares_json JSON NULL,
                        preferred_scenes_json JSON NULL,
                        onboarding_completed TINYINT DEFAULT 0,
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_user_preference_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好画像表'
                    """);
            log.info("user_preference_profiles 表创建完成");
        } catch (Exception e) {
            log.warn("创建 user_preference_profiles 表失败：{}", e.getMessage());
        }
    }

    private void addBehaviorEventsTable() {
        try {
            if (checkTableExists("behavior_events")) {
                log.info("behavior_events 表已存在，跳过创建");
                return;
            }
            log.info("创建 behavior_events 表...");
            jdbcTemplate.execute("""
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
                        CONSTRAINT fk_behavior_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                        CONSTRAINT fk_behavior_recipe
                            FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE SET NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为埋点事件表'
                    """);
            log.info("behavior_events 表创建完成");
        } catch (Exception e) {
            log.warn("创建 behavior_events 表失败：{}", e.getMessage());
        }
    }

    private void addCommentLikesTable() {
        try {
            if (checkTableExists("comment_likes")) {
                log.info("comment_likes 表已存在，跳过创建");
                return;
            }
            log.info("创建 comment_likes 表...");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS comment_likes (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        comment_id INT NOT NULL,
                        user_id INT NOT NULL,
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_comment_like_comment
                            FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
                        CONSTRAINT fk_comment_like_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE KEY uk_comment_like_user_comment (user_id, comment_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞明细表'
                    """);
            log.info("comment_likes 表创建完成");
        } catch (Exception e) {
            log.warn("创建 comment_likes 表失败：{}", e.getMessage());
        }
    }

    private void addCookingSessionsTable() {
        try {
            if (checkTableExists("cooking_sessions")) {
                log.info("cooking_sessions 表已存在，跳过创建");
                return;
            }
            log.info("创建 cooking_sessions 表...");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS cooking_sessions (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        recipe_id INT NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'in_progress',
                        current_step INT DEFAULT 1,
                        total_steps INT DEFAULT 0,
                        duration_ms INT DEFAULT 0,
                        started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        finished_at DATETIME NULL,
                        CONSTRAINT fk_cooking_session_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_cooking_session_recipe
                            FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户烹饪会话表'
                    """);
            log.info("cooking_sessions 表创建完成");
        } catch (Exception e) {
            log.warn("创建 cooking_sessions 表失败：{}", e.getMessage());
        }
    }

    private void addDailyRecommendationTables() {
        try {
            if (!checkTableExists("daily_recipe_recommendations")) {
                log.info("创建 daily_recipe_recommendations 表...");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS daily_recipe_recommendations (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            biz_date DATE NOT NULL,
                            recipe_id INT NOT NULL,
                            rank_no INT NOT NULL,
                            selected_for_delivery TINYINT DEFAULT 0,
                            model_score DECIMAL(16,8) DEFAULT 0,
                            reason_json JSON NULL,
                            model_version VARCHAR(64) NOT NULL,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_daily_reco_user
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_daily_reco_recipe
                                FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
                            UNIQUE KEY uk_daily_reco_user_date_rank (user_id, biz_date, rank_no),
                            UNIQUE KEY uk_daily_reco_user_date_recipe (user_id, biz_date, recipe_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐结果表'
                        """);
                log.info("daily_recipe_recommendations 表创建完成");
            } else {
                log.info("daily_recipe_recommendations 表已存在，跳过创建");
            }

            if (!checkTableExists("daily_recommend_job_runs")) {
                log.info("创建 daily_recommend_job_runs 表...");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS daily_recommend_job_runs (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            job_date DATE NOT NULL,
                            phase VARCHAR(32) NOT NULL,
                            model_version VARCHAR(64) NOT NULL,
                            affected_users INT DEFAULT 0,
                            affected_recipes INT DEFAULT 0,
                            status VARCHAR(20) NOT NULL DEFAULT 'running',
                            error_message TEXT NULL,
                            started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            finished_at DATETIME NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐任务执行记录表'
                        """);
                log.info("daily_recommend_job_runs 表创建完成");
            } else {
                log.info("daily_recommend_job_runs 表已存在，跳过创建");
            }

            addIndexIfNotExists("idx_daily_reco_user_date_delivery",
                    "daily_recipe_recommendations",
                    "user_id, biz_date, selected_for_delivery, rank_no");
            addIndexIfNotExists("idx_daily_reco_date_version",
                    "daily_recipe_recommendations",
                    "biz_date, model_version");
            addIndexIfNotExists("idx_daily_job_runs_date_phase",
                    "daily_recommend_job_runs",
                    "job_date, phase, status");
            addIndexIfNotExists("idx_daily_job_runs_started",
                    "daily_recommend_job_runs",
                    "started_at DESC");
        } catch (Exception e) {
            log.warn("创建每日推荐相关表失败：{}", e.getMessage());
        }
    }

    private void analyzeTables() {
        try {
            log.info("分析表以更新统计信息...");
            jdbcTemplate.execute("ANALYZE TABLE users");
            jdbcTemplate.execute("ANALYZE TABLE comments");
            jdbcTemplate.execute("ANALYZE TABLE interactions");
            jdbcTemplate.execute("ANALYZE TABLE behavior_events");
            jdbcTemplate.execute("ANALYZE TABLE cooking_sessions");
            log.info("表分析完成");
        } catch (Exception e) {
            log.warn("分析表时出错：{}", e.getMessage());
        }
    }
}
