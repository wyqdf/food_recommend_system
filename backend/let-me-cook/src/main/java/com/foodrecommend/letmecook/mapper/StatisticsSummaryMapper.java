package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.*;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatisticsSummaryMapper {

    // ==================== 统计概览 ====================
    @Select("SELECT stat_date, total_users, total_recipes, total_categories, total_comments, " +
            "today_views, today_new_users, today_new_recipes, last_updated " +
            "FROM statistics_overview WHERE stat_date = #{date} LIMIT 1")
    StatisticsOverview getOverviewByDate(@Param("date") LocalDate date);

    @Select("SELECT stat_date, total_users, total_recipes, total_categories, total_comments, " +
            "today_views, today_new_users, today_new_recipes, last_updated " +
            "FROM statistics_overview ORDER BY stat_date DESC LIMIT 1")
    StatisticsOverview getLatestOverview();

    // ==================== 用户趋势 ====================
    @Select("SELECT stat_date, new_users_count, total_users, last_updated " +
            "FROM user_trend_daily WHERE stat_date >= #{startDate} ORDER BY stat_date")
    List<UserTrendDaily> getUserTrendByDateRange(@Param("startDate") LocalDate startDate);

    @Select("SELECT stat_date, new_users_count, total_users, last_updated " +
            "FROM user_trend_daily ORDER BY stat_date DESC LIMIT 7")
    List<UserTrendDaily> getLatestUserTrend();

    // ==================== 食谱趋势 ====================
    @Select("SELECT stat_date, new_recipes_count, total_recipes, last_updated " +
            "FROM recipe_trend_daily WHERE stat_date >= #{startDate} ORDER BY stat_date")
    List<RecipeTrendDaily> getRecipeTrendByDateRange(@Param("startDate") LocalDate startDate);

    @Select("SELECT stat_date, new_recipes_count, total_recipes, last_updated " +
            "FROM recipe_trend_daily ORDER BY stat_date DESC LIMIT 7")
    List<RecipeTrendDaily> getLatestRecipeTrend();

    // ==================== 评论趋势 ====================
    @Select("SELECT stat_date, new_comments_count, total_comments, last_updated " +
            "FROM comment_trend_daily WHERE stat_date >= #{startDate} ORDER BY stat_date")
    List<CommentTrendDaily> getCommentTrendByDateRange(@Param("startDate") LocalDate startDate);

    @Select("SELECT stat_date, new_comments_count, total_comments, last_updated " +
            "FROM comment_trend_daily ORDER BY stat_date DESC LIMIT 7")
    List<CommentTrendDaily> getLatestCommentTrend();

    // ==================== 分类分布 ====================
    @Select("SELECT stat_date, category_id, category_name, recipe_count, last_updated " +
            "FROM category_distribution_summary WHERE stat_date = #{date} ORDER BY recipe_count DESC LIMIT 10")
    List<CategoryDistributionSummary> getCategoryDistributionByDate(@Param("date") LocalDate date);

    @Select("SELECT stat_date, category_id, category_name, recipe_count, last_updated " +
            "FROM category_distribution_summary WHERE stat_date = (SELECT MAX(stat_date) FROM category_distribution_summary) " +
            "ORDER BY recipe_count DESC LIMIT 10")
    List<CategoryDistributionSummary> getLatestCategoryDistribution();

    // ==================== 难度分布 ====================
    @Select("SELECT stat_date, difficulty_id, difficulty_name, recipe_count, last_updated " +
            "FROM difficulty_distribution_summary WHERE stat_date = #{date} ORDER BY difficulty_id")
    List<DifficultyDistributionSummary> getDifficultyDistributionByDate(@Param("date") LocalDate date);

    @Select("SELECT stat_date, difficulty_id, difficulty_name, recipe_count, last_updated " +
            "FROM difficulty_distribution_summary WHERE stat_date = (SELECT MAX(stat_date) FROM difficulty_distribution_summary) " +
            "ORDER BY difficulty_id")
    List<DifficultyDistributionSummary> getLatestDifficultyDistribution();

    // ==================== 热门食谱 ====================
    @Select("SELECT " +
            "stat_time, recipe_id, recipe_title, like_count, view_count, comment_count, recipe_rank, last_updated " +
            "FROM top_recipes_hourly " +
            "WHERE stat_time = (SELECT MAX(stat_time) FROM top_recipes_hourly) " +
            "ORDER BY recipe_rank LIMIT #{limit}")
    List<TopRecipesHourly> getLatestTopRecipes(@Param("limit") int limit);

    // ==================== 热门评论食谱 ====================
    @Select("SELECT stat_time, recipe_id, recipe_title, comment_count, recipe_rank, last_updated " +
            "FROM top_commented_recipes_hourly " +
            "WHERE stat_time = (SELECT MAX(stat_time) FROM top_commented_recipes_hourly) " +
            "ORDER BY recipe_rank LIMIT #{limit}")
    List<TopCommentedRecipesHourly> getLatestTopCommentedRecipes(@Param("limit") int limit);

    // ==================== 插入/更新操作 ====================

    // 插入或更新统计概览
    @Insert("<script>" +
            "INSERT INTO statistics_overview " +
            "(stat_date, total_users, total_recipes, total_categories, total_comments, " +
            " today_views, today_new_users, today_new_recipes) " +
            "VALUES " +
            "(#{statDate}, COALESCE(#{totalUsers}, 0), COALESCE(#{totalRecipes}, 0), " +
            " COALESCE(#{totalCategories}, 0), COALESCE(#{totalComments}, 0), " +
            " COALESCE(#{todayViews}, 0), COALESCE(#{todayNewUsers}, 0), COALESCE(#{todayNewRecipes}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_users = VALUES(total_users), " +
            "total_recipes = VALUES(total_recipes), " +
            "total_categories = VALUES(total_categories), " +
            "total_comments = VALUES(total_comments), " +
            "today_views = VALUES(today_views), " +
            "today_new_users = VALUES(today_new_users), " +
            "today_new_recipes = VALUES(today_new_recipes), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateOverview(StatisticsOverview overview);

    // 插入或更新用户趋势
    @Insert("<script>" +
            "INSERT INTO user_trend_daily (stat_date, new_users_count, total_users) " +
            "VALUES (#{statDate}, COALESCE(#{newUsersCount}, 0), COALESCE(#{totalUsers}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "new_users_count = VALUES(new_users_count), " +
            "total_users = VALUES(total_users), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateUserTrend(UserTrendDaily trend);

    // 插入或更新食谱趋势
    @Insert("<script>" +
            "INSERT INTO recipe_trend_daily (stat_date, new_recipes_count, total_recipes) " +
            "VALUES (#{statDate}, COALESCE(#{newRecipesCount}, 0), COALESCE(#{totalRecipes}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "new_recipes_count = VALUES(new_recipes_count), " +
            "total_recipes = VALUES(total_recipes), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateRecipeTrend(RecipeTrendDaily trend);

    // 插入或更新评论趋势
    @Insert("<script>" +
            "INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments) " +
            "VALUES (#{statDate}, COALESCE(#{newCommentsCount}, 0), COALESCE(#{totalComments}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "new_comments_count = VALUES(new_comments_count), " +
            "total_comments = VALUES(total_comments), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateCommentTrend(CommentTrendDaily trend);

    // 插入或更新分类分布
    @Insert("<script>" +
            "INSERT INTO category_distribution_summary (stat_date, category_id, category_name, recipe_count) " +
            "VALUES (#{statDate}, #{categoryId}, #{categoryName}, COALESCE(#{recipeCount}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "recipe_count = VALUES(recipe_count), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateCategoryDistribution(CategoryDistributionSummary summary);

    // 插入或更新难度分布
    @Insert("<script>" +
            "INSERT INTO difficulty_distribution_summary (stat_date, difficulty_id, difficulty_name, recipe_count) " +
            "VALUES (#{statDate}, #{difficultyId}, #{difficultyName}, COALESCE(#{recipeCount}, 0)) " +
            "ON DUPLICATE KEY UPDATE " +
            "recipe_count = VALUES(recipe_count), " +
            "last_updated = NOW()" +
            "</script>")
    void saveOrUpdateDifficultyDistribution(DifficultyDistributionSummary summary);

    // 插入热门食谱（每天清空后重新插入）
    @Insert("<script>" +
            "INSERT INTO top_recipes_hourly " +
            "(stat_time, recipe_id, recipe_title, like_count, view_count, comment_count, recipe_rank) " +
            "VALUES " +
            "(#{statTime}, #{recipeId}, #{recipeTitle}, COALESCE(#{likeCount}, 0), COALESCE(#{viewCount}, 0), " +
            " COALESCE(#{commentCount}, 0), #{recipeRank}) " +
            "ON DUPLICATE KEY UPDATE " +
            "like_count = VALUES(like_count), " +
            "view_count = VALUES(view_count), " +
            "comment_count = VALUES(comment_count), " +
            "recipe_rank = VALUES(recipe_rank), " +
            "last_updated = NOW()" +
            "</script>")
    void insertTopRecipe(TopRecipesHourly recipe);

    // 插入热门评论食谱
    @Insert("<script>" +
            "INSERT INTO top_commented_recipes_hourly " +
            "(stat_time, recipe_id, recipe_title, comment_count, recipe_rank) " +
            "VALUES " +
            "(#{statTime}, #{recipeId}, #{recipeTitle}, COALESCE(#{commentCount}, 0), #{recipeRank}) " +
            "ON DUPLICATE KEY UPDATE " +
            "comment_count = VALUES(comment_count), " +
            "recipe_rank = VALUES(recipe_rank), " +
            "last_updated = NOW()" +
            "</script>")
    void insertTopCommentedRecipe(TopCommentedRecipesHourly recipe);

    // 删除指定时间之前的热门食谱
    @Delete("DELETE FROM top_recipes_hourly WHERE stat_time &lt; #{cutoffTime}")
    void deleteOldTopRecipes(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 删除指定时间之前的热门评论食谱
    @Delete("DELETE FROM top_commented_recipes_hourly WHERE stat_time &lt; #{cutoffTime}")
    void deleteOldTopCommentedRecipes(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 清理 N 天前的趋势数据
    @Delete("DELETE FROM user_trend_daily WHERE stat_date &lt; #{cutoffDate}")
    void deleteOldUserTrend(@Param("cutoffDate") LocalDate cutoffDate);

    @Delete("DELETE FROM recipe_trend_daily WHERE stat_date &lt; #{cutoffDate}")
    void deleteOldRecipeTrend(@Param("cutoffDate") LocalDate cutoffDate);

    @Delete("DELETE FROM comment_trend_daily WHERE stat_date &lt; #{cutoffDate}")
    void deleteOldCommentTrend(@Param("cutoffDate") LocalDate cutoffDate);

    // ==================== Advanced Statistics Summary ====================

    // Ingredient usage
    @Select("SELECT * FROM ingredient_usage_summary WHERE stat_date = (SELECT MAX(stat_date) FROM ingredient_usage_summary) ORDER BY recipe_count DESC LIMIT #{limit}")
    List<IngredientUsageSummary> getLatestIngredientUsage(@Param("limit") int limit);

    @Insert("<script>INSERT INTO ingredient_usage_summary (stat_date, ingredient_id, ingredient_name, recipe_count) VALUES (#{statDate}, #{ingredientId}, #{ingredientName}, #{recipeCount}) ON DUPLICATE KEY UPDATE recipe_count = VALUES(recipe_count), last_updated = NOW()</script>")
    void saveOrUpdateIngredientUsage(IngredientUsageSummary summary);

    // Taste distribution
    @Select("SELECT * FROM taste_distribution_summary WHERE stat_date = (SELECT MAX(stat_date) FROM taste_distribution_summary) ORDER BY recipe_count DESC")
    List<TasteDistributionSummary> getLatestTasteDistribution();

    @Insert("<script>INSERT INTO taste_distribution_summary (stat_date, taste_id, taste_name, recipe_count) VALUES (#{statDate}, #{tasteId}, #{tasteName}, #{recipeCount}) ON DUPLICATE KEY UPDATE recipe_count = VALUES(recipe_count), last_updated = NOW()</script>")
    void saveOrUpdateTasteDistribution(TasteDistributionSummary summary);

    // Technique distribution
    @Select("SELECT * FROM technique_distribution_summary WHERE stat_date = (SELECT MAX(stat_date) FROM technique_distribution_summary) ORDER BY recipe_count DESC")
    List<TechniqueDistributionSummary> getLatestTechniqueDistribution();

    @Insert("<script>INSERT INTO technique_distribution_summary (stat_date, technique_id, technique_name, recipe_count) VALUES (#{statDate}, #{techniqueId}, #{techniqueName}, #{recipeCount}) ON DUPLICATE KEY UPDATE recipe_count = VALUES(recipe_count), last_updated = NOW()</script>")
    void saveOrUpdateTechniqueDistribution(TechniqueDistributionSummary summary);

    // TimeCost distribution
    @Select("SELECT * FROM timecost_distribution_summary WHERE stat_date = (SELECT MAX(stat_date) FROM timecost_distribution_summary) ORDER BY timecost_id")
    List<TimeCostDistributionSummary> getLatestTimeCostDistribution();

    @Insert("<script>INSERT INTO timecost_distribution_summary (stat_date, timecost_id, timecost_name, recipe_count) VALUES (#{statDate}, #{timecostId}, #{timecostName}, #{recipeCount}) ON DUPLICATE KEY UPDATE recipe_count = VALUES(recipe_count), last_updated = NOW()</script>")
    void saveOrUpdateTimeCostDistribution(TimeCostDistributionSummary summary);

    // Active users
    @Select("SELECT * FROM active_users_summary WHERE stat_date = (SELECT MAX(stat_date) FROM active_users_summary) ORDER BY user_rank LIMIT #{limit}")
    List<ActiveUsersSummary> getLatestActiveUsers(@Param("limit") int limit);

    @Insert("<script>INSERT INTO active_users_summary (stat_date, user_id, username, recipe_count, comment_count, total_score, user_rank) VALUES (#{statDate}, #{userId}, #{username}, #{recipeCount}, #{commentCount}, #{totalScore}, #{userRank}) ON DUPLICATE KEY UPDATE username = VALUES(username), recipe_count = VALUES(recipe_count), comment_count = VALUES(comment_count), total_score = VALUES(total_score), user_rank = VALUES(user_rank), last_updated = NOW()</script>")
    void saveOrUpdateActiveUser(ActiveUsersSummary summary);

    // Quality analysis
    @Select("SELECT * FROM quality_analysis_summary ORDER BY stat_date DESC LIMIT 1")
    QualityAnalysisSummary getLatestQualityAnalysis();

    @Insert("<script>INSERT INTO quality_analysis_summary (stat_date, high_quality_recipes, average_like_count, average_comment_count, zero_interaction_recipes, total_recipes) VALUES (#{statDate}, #{highQualityRecipes}, #{averageLikeCount}, #{averageCommentCount}, #{zeroInteractionRecipes}, #{totalRecipes}) ON DUPLICATE KEY UPDATE high_quality_recipes = VALUES(high_quality_recipes), average_like_count = VALUES(average_like_count), average_comment_count = VALUES(average_comment_count), zero_interaction_recipes = VALUES(zero_interaction_recipes), total_recipes = VALUES(total_recipes), last_updated = NOW()</script>")
    void saveOrUpdateQualityAnalysis(QualityAnalysisSummary summary);

    // Interaction trend
    @Select("SELECT * FROM interaction_trend_summary WHERE stat_date >= #{startDate} ORDER BY stat_date")
    List<InteractionTrendSummary> getInteractionTrendByDateRange(@Param("startDate") LocalDate startDate);

    @Insert("<script>INSERT INTO interaction_trend_summary (stat_date, like_count, favorite_count, view_count) VALUES (#{statDate}, #{likeCount}, #{favoriteCount}, #{viewCount}) ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), favorite_count = VALUES(favorite_count), view_count = VALUES(view_count), last_updated = NOW()</script>")
    void saveOrUpdateInteractionTrend(InteractionTrendSummary summary);
}
