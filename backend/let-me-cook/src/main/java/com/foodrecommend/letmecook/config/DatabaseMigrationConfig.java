package com.foodrecommend.letmecook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Bean
    public CommandLineRunner addPerformanceIndexes() {
        return args -> {
            log.info("开始执行数据库迁移和性能优化...");
            
            addCookwaresTable();
            addRecipeCountColumns();
            enhanceOperationLogsTable();
            
            log.info("开始执行数据库性能优化索引...");
            addIndexIfNotExists("idx_comment_user_create", "comments", "user_id, create_time");
            addIndexIfNotExists("idx_interaction_user_type", "interactions", "user_id, interaction_type");
            addIndexIfNotExists("idx_user_status", "users", "status");
            addIndexIfNotExists("idx_user_create_time", "users", "create_time DESC");
            
            analyzeTables();
            
            log.info("数据库迁移和性能优化执行完成");
        };
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
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'food_recommend' AND TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
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
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'food_recommend' AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
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

    private boolean checkIndexExists(String tableName, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'food_recommend' AND TABLE_NAME = ? AND index_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, indexName);
        return count != null && count > 0;
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

    private void analyzeTables() {
        try {
            log.info("分析表以更新统计信息...");
            jdbcTemplate.execute("ANALYZE TABLE users");
            jdbcTemplate.execute("ANALYZE TABLE comments");
            jdbcTemplate.execute("ANALYZE TABLE interactions");
            log.info("表分析完成");
        } catch (Exception e) {
            log.warn("分析表时出错：{}", e.getMessage());
        }
    }
}
