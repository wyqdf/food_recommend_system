package com.foodrecommend.letmecook.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

        @Select("SELECT COUNT(*) FROM users")
    int countTotalUsers();

    @Select("SELECT COUNT(*) FROM recipes")
    int countTotalRecipes();

    @Select("SELECT COUNT(*) FROM categories")
    int countTotalCategories();

    @Select("SELECT COUNT(*) FROM comments")
    int countTotalComments();

    @Select("SELECT COUNT(*) FROM interactions WHERE DATE(create_time) = CURDATE()")
    int countTodayViews();

    @Select("SELECT COUNT(*) FROM users WHERE DATE(create_time) = CURDATE()")
    int countTodayNewUsers();

    @Select("SELECT COUNT(*) FROM recipes WHERE DATE(create_time) = CURDATE()")
    int countTodayNewRecipes();

        @Select("<script>" +
            "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM users " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "ORDER BY date" +
            "</script>")
    List<Map<String, Object>> getUserTrend();

    @Select("<script>" +
            "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM recipes " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "ORDER BY date" +
            "</script>")
    List<Map<String, Object>> getRecipeTrend();

    @Select("<script>" +
            "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM comments " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "ORDER BY date" +
            "</script>")
    List<Map<String, Object>> getCommentTrend();

        @Select("SELECT " +
            "c.id AS category_id, " +
            "c.name AS category_name, " +
            "c.name AS name, " +
            "COUNT(rc.recipe_id) AS recipe_count, " +
            "COUNT(rc.recipe_id) AS value " +
            "FROM categories c " +
            "LEFT JOIN recipe_categories rc ON c.id = rc.category_id " +
            "GROUP BY c.id, c.name " +
            "ORDER BY value DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getCategoryDistribution();

    @Select("SELECT " +
            "d.id AS difficulty_id, " +
            "d.name AS difficulty_name, " +
            "d.name AS name, " +
            "COUNT(r.id) AS recipe_count, " +
            "COUNT(r.id) AS value " +
            "FROM difficulties d " +
            "LEFT JOIN recipes r ON d.id = r.difficulty_id " +
            "GROUP BY d.id, d.name " +
            "ORDER BY d.id")
    List<Map<String, Object>> getDifficultyDistribution();

        @Select("<script>" +
            "SELECT r.id, r.title, r.like_count, r.rating_count as view_count, r.reply_count as comment_count " +
            "FROM recipes r " +
            "ORDER BY r.like_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> getTopRecipes(@Param("limit") int limit);

        @Select("<script>" +
            "SELECT r.id, r.title, COUNT(c.id) as comment_count " +
            "FROM recipes r " +
            "LEFT JOIN comments c ON r.id = c.recipe_id " +
            "GROUP BY r.id, r.title " +
            "ORDER BY comment_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> getTopCommentedRecipes(@Param("limit") int limit);
}
