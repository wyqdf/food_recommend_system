package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.dto.admin.AdvancedStatisticsDTO.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdvancedStatisticsMapper {

        // ==================== 食材使用统计 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  i.id as ingredient_id, " +
                        "  i.name as ingredient_name, " +
                        "  COUNT(DISTINCT ri.recipe_id) as recipe_count " +
                        "FROM ingredients i " +
                        "INNER JOIN recipe_ingredients ri ON i.id = ri.ingredient_id " +
                        "GROUP BY i.id, i.name " +
                        "ORDER BY recipe_count DESC " +
                        "LIMIT #{limit}" +
                        "</script>")
        List<Map<String, Object>> getTopIngredients(@Param("limit") int limit);

        // ==================== 口味分布统计 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  t.id as taste_id, " +
                        "  t.name as taste_name, " +
                        "  COUNT(r.id) as recipe_count " +
                        "FROM tastes t " +
                        "LEFT JOIN recipes r ON t.id = r.taste_id " +
                        "GROUP BY t.id, t.name " +
                        "ORDER BY recipe_count DESC" +
                        "</script>")
        List<Map<String, Object>> getTasteDistribution();

        // ==================== 烹饪技法统计 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  tec.id as technique_id, " +
                        "  tec.name as technique_name, " +
                        "  COUNT(r.id) as recipe_count " +
                        "FROM techniques tec " +
                        "LEFT JOIN recipes r ON tec.id = r.technique_id " +
                        "GROUP BY tec.id, tec.name " +
                        "ORDER BY recipe_count DESC" +
                        "</script>")
        List<Map<String, Object>> getTechniqueDistribution();

        // ==================== 互动趋势分析（近 7 天） ====================
        @Select("<script>" +
                        "SELECT " +
                        "  DATE(create_time) as date, " +
                        "  SUM(CASE WHEN interaction_type = 'like' THEN 1 ELSE 0 END) as like_count, " +
                        "  SUM(CASE WHEN interaction_type = 'favorite' THEN 1 ELSE 0 END) as favorite_count, " +
                        "  SUM(CASE WHEN interaction_type = 'view' THEN 1 ELSE 0 END) as view_count " +
                        "FROM interactions " +
                        "WHERE create_time &gt;= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                        "GROUP BY DATE(create_time) " +
                        "ORDER BY date" +
                        "</script>")
        List<Map<String, Object>> getInteractionTrend();

        // ==================== 活跃用户统计 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  u.id as user_id, " +
                        "  u.username, " +
                        "  COALESCE(r.recipe_count, 0) as recipe_count, " +
                        "  COALESCE(c.comment_count, 0) as comment_count, " +
                        "  (COALESCE(r.recipe_count, 0) * 10 + COALESCE(c.comment_count, 0) * 5) as total_score " +
                        "FROM users u " +
                        "LEFT JOIN (SELECT CAST(author_uid AS UNSIGNED) as user_id, COUNT(*) as recipe_count FROM recipes WHERE author_uid IS NOT NULL AND author_uid != '' GROUP BY author_uid) r ON u.id = r.user_id "
                        +
                        "LEFT JOIN (SELECT user_id, COUNT(*) as comment_count FROM comments WHERE user_id IS NOT NULL GROUP BY user_id) c ON u.id = c.user_id "
                        +
                        "WHERE COALESCE(r.recipe_count, 0) > 0 OR COALESCE(c.comment_count, 0) > 0 " +
                        "ORDER BY total_score DESC " +
                        "LIMIT #{limit}" +
                        "</script>")
        List<Map<String, Object>> getTopActiveUsers(@Param("limit") int limit);

        // ==================== 食谱质量分析 ====================
        @Select("SELECT " +
                        "  COUNT(CASE WHEN like_count > 100 THEN 1 END) as high_quality_recipes, " +
                        "  AVG(like_count) as average_like_count, " +
                        "  AVG(reply_count) as average_comment_count, " +
                        "  COUNT(CASE WHEN like_count = 0 AND reply_count = 0 THEN 1 END) as zero_interaction_recipes "
                        +
                        "FROM recipes")
        Map<String, Object> getQualityAnalysis();

        // ==================== 难度分布（补充） ====================
        @Select("<script>" +
                        "SELECT " +
                        "  d.id as difficulty_id, " +
                        "  d.name as difficulty_name, " +
                        "  COUNT(r.id) as recipe_count " +
                        "FROM difficulties d " +
                        "LEFT JOIN recipes r ON d.id = r.difficulty_id " +
                        "GROUP BY d.id, d.name " +
                        "ORDER BY d.id" +
                        "</script>")
        List<Map<String, Object>> getDifficultyDistributionWithPercentage();

        // ==================== 耗时分布统计 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  tc.id as time_cost_id, " +
                        "  tc.name as time_cost_name, " +
                        "  COUNT(r.id) as recipe_count " +
                        "FROM time_costs tc " +
                        "LEFT JOIN recipes r ON tc.id = r.time_cost_id " +
                        "GROUP BY tc.id, tc.name " +
                        "ORDER BY tc.id" +
                        "</script>")
        List<Map<String, Object>> getTimeCostDistribution();

        // ==================== 月度新增趋势 ====================
        @Select("<script>" +
                        "SELECT " +
                        "  DATE_FORMAT(create_time, '%Y-%m') as month, " +
                        "  COUNT(*) as count " +
                        "FROM recipes " +
                        "WHERE create_time &gt;= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                        "GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
                        "ORDER BY month" +
                        "</script>")
        List<Map<String, Object>> getMonthlyRecipeTrend();

        @Select("<script>" +
                        "SELECT " +
                        "  DATE_FORMAT(create_time, '%Y-%m') as month, " +
                        "  COUNT(*) as count " +
                        "FROM users " +
                        "WHERE create_time &gt;= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                        "GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
                        "ORDER BY month" +
                        "</script>")
        List<Map<String, Object>> getMonthlyUserTrend();

        @Select("<script>" +
                        "SELECT " +
                        "  DATE_FORMAT(create_time, '%Y-%m') as month, " +
                        "  COUNT(*) as count " +
                        "FROM comments " +
                        "WHERE create_time &gt;= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                        "GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
                        "ORDER BY month" +
                        "</script>")
        List<Map<String, Object>> getMonthlyCommentTrend();
}
