package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.AdvancedStatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsSummaryMapper;
import com.foodrecommend.letmecook.util.MapValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsRefreshService {

    private final StatisticsSummaryMapper statisticsSummaryMapper;
    private final StatisticsMapper statisticsMapper;
    private final AdvancedStatisticsMapper advancedStatisticsMapper;

    /**
     * 每小时更新一次统计数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void hourlyRefresh() {
        log.info("Starting hourly statistics refresh task...");
        long startTime = System.currentTimeMillis();

        try {
            refreshStatisticsOverview();
            refreshTrendData();
            refreshDistributionData();
            refreshTopLists();
            refreshAdvancedStatistics();

            long endTime = System.currentTimeMillis();
            log.info("Statistics refresh completed in {}ms", (endTime - startTime));
        } catch (Exception e) {
            log.error("Statistics refresh failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天凌晨 2 点清理旧数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyCleanup() {
        log.info("开始执行每日数据清理任务...");
        long startTime = System.currentTimeMillis();

        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            statisticsSummaryMapper.deleteOldUserTrend(cutoffDate);
            statisticsSummaryMapper.deleteOldRecipeTrend(cutoffDate);
            statisticsSummaryMapper.deleteOldCommentTrend(cutoffDate);

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            statisticsSummaryMapper.deleteOldTopRecipes(cutoffTime);
            statisticsSummaryMapper.deleteOldTopCommentedRecipes(cutoffTime);

            long endTime = System.currentTimeMillis();
            log.info("数据清理任务执行完成，耗时：{}ms", (endTime - startTime));
        } catch (Exception e) {
            log.error("数据清理任务执行失败：{}", e.getMessage(), e);
        }
    }

    private void refreshStatisticsOverview() {
        log.info("更新统计概览数据...");

        int totalUsers = statisticsMapper.countTotalUsers();
        int totalRecipes = statisticsMapper.countTotalRecipes();
        int totalCategories = statisticsMapper.countTotalCategories();
        int totalComments = statisticsMapper.countTotalComments();
        int todayViews = statisticsMapper.countTodayViews();
        int todayNewUsers = statisticsMapper.countTodayNewUsers();
        int todayNewRecipes = statisticsMapper.countTodayNewRecipes();

        StatisticsOverview overview = new StatisticsOverview();
        overview.setStatDate(LocalDate.now());
        overview.setTotalUsers(totalUsers);
        overview.setTotalRecipes(totalRecipes);
        overview.setTotalCategories(totalCategories);
        overview.setTotalComments(totalComments);
        overview.setTodayViews(todayViews);
        overview.setTodayNewUsers(todayNewUsers);
        overview.setTodayNewRecipes(todayNewRecipes);

        statisticsSummaryMapper.saveOrUpdateOverview(overview);

        log.info("统计概览更新完成：用户={}, 食谱={}, 分类={}, 评论={}",
                totalUsers, totalRecipes, totalCategories, totalComments);
    }

    private void refreshTrendData() {
        log.info("更新趋势数据...");

        updateLast7DaysUserTrend();
        updateLast7DaysRecipeTrend();
        updateLast7DaysCommentTrend();

        log.info("趋势数据更新完成");
    }

    private void refreshDistributionData() {
        log.info("更新分布数据...");

        updateCategoryDistribution();
        updateDifficultyDistribution();

        log.info("分布数据更新完成");
    }

    private void refreshTopLists() {
        log.info("更新热门榜单...");

        updateTopRecipes();
        updateTopCommentedRecipes();

        log.info("热门榜单更新完成");
    }

    private void updateLast7DaysUserTrend() {
        List<Map<String, Object>> trendData = statisticsMapper.getUserTrend();
        for (Map<String, Object> item : trendData) {
            LocalDate statDate = MapValueUtils.getLocalDate(item, "date");
            if (statDate == null) {
                continue;
            }
            UserTrendDaily trend = new UserTrendDaily();
            trend.setStatDate(statDate);
            trend.setNewUsersCount(MapValueUtils.getIntOrDefault(item, 0, "count"));
            statisticsSummaryMapper.saveOrUpdateUserTrend(trend);
        }
    }

    private void updateLast7DaysRecipeTrend() {
        List<Map<String, Object>> trendData = statisticsMapper.getRecipeTrend();
        for (Map<String, Object> item : trendData) {
            LocalDate statDate = MapValueUtils.getLocalDate(item, "date");
            if (statDate == null) {
                continue;
            }
            RecipeTrendDaily trend = new RecipeTrendDaily();
            trend.setStatDate(statDate);
            trend.setNewRecipesCount(MapValueUtils.getIntOrDefault(item, 0, "count"));
            statisticsSummaryMapper.saveOrUpdateRecipeTrend(trend);
        }
    }

    private void updateLast7DaysCommentTrend() {
        List<Map<String, Object>> trendData = statisticsMapper.getCommentTrend();
        for (Map<String, Object> item : trendData) {
            LocalDate statDate = MapValueUtils.getLocalDate(item, "date");
            if (statDate == null) {
                continue;
            }
            CommentTrendDaily trend = new CommentTrendDaily();
            trend.setStatDate(statDate);
            trend.setNewCommentsCount(MapValueUtils.getIntOrDefault(item, 0, "count"));
            statisticsSummaryMapper.saveOrUpdateCommentTrend(trend);
        }
    }

    private void updateCategoryDistribution() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> distribution = statisticsMapper.getCategoryDistribution();
        for (Map<String, Object> item : distribution) {
            CategoryDistributionSummary summary = new CategoryDistributionSummary();
            summary.setStatDate(today);
            summary.setCategoryId(MapValueUtils.getInt(item, "category_id"));
            summary.setCategoryName(MapValueUtils.getString(item, "category_name", "name"));
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count", "value"));
            if (summary.getCategoryId() != null && summary.getCategoryName() != null) {
                statisticsSummaryMapper.saveOrUpdateCategoryDistribution(summary);
            }
        }
    }

    private void updateDifficultyDistribution() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> distribution = statisticsMapper.getDifficultyDistribution();
        for (Map<String, Object> item : distribution) {
            DifficultyDistributionSummary summary = new DifficultyDistributionSummary();
            summary.setStatDate(today);
            summary.setDifficultyId(MapValueUtils.getInt(item, "difficulty_id"));
            summary.setDifficultyName(MapValueUtils.getString(item, "difficulty_name", "name"));
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count", "value"));
            if (summary.getDifficultyId() != null && summary.getDifficultyName() != null) {
                statisticsSummaryMapper.saveOrUpdateDifficultyDistribution(summary);
            }
        }
    }

    private void updateTopRecipes() {
        LocalDateTime statTime = LocalDateTime.now();
        List<Map<String, Object>> topRecipes = statisticsMapper.getTopRecipes(10);

        int rank = 1;
        for (Map<String, Object> item : topRecipes) {
            TopRecipesHourly recipe = new TopRecipesHourly();
            recipe.setStatTime(statTime);
            recipe.setRecipeId(MapValueUtils.getInt(item, "id", "recipe_id"));
            recipe.setRecipeTitle(MapValueUtils.getString(item, "title", "recipe_title"));
            recipe.setLikeCount(MapValueUtils.getIntOrDefault(item, 0, "like_count"));
            recipe.setViewCount(MapValueUtils.getIntOrDefault(item, 0, "view_count"));
            recipe.setCommentCount(MapValueUtils.getIntOrDefault(item, 0, "comment_count"));
            if (recipe.getRecipeId() == null || recipe.getRecipeTitle() == null || recipe.getRecipeTitle().isBlank()) {
                continue;
            }
            recipe.setRecipeRank(rank++);

            statisticsSummaryMapper.insertTopRecipe(recipe);
        }
    }

    private void updateTopCommentedRecipes() {
        LocalDateTime statTime = LocalDateTime.now();
        List<Map<String, Object>> topRecipes = statisticsMapper.getTopCommentedRecipes(10);

        int rank = 1;
        for (Map<String, Object> item : topRecipes) {
            TopCommentedRecipesHourly recipe = new TopCommentedRecipesHourly();
            recipe.setStatTime(statTime);
            recipe.setRecipeId(MapValueUtils.getInt(item, "id", "recipe_id"));
            recipe.setRecipeTitle(MapValueUtils.getString(item, "title", "recipe_title"));
            recipe.setCommentCount(MapValueUtils.getIntOrDefault(item, 0, "comment_count"));
            if (recipe.getRecipeId() == null || recipe.getRecipeTitle() == null || recipe.getRecipeTitle().isBlank()) {
                continue;
            }
            recipe.setRecipeRank(rank++);

            statisticsSummaryMapper.insertTopCommentedRecipe(recipe);
        }
    }

    private void refreshAdvancedStatistics() {
        log.info("Refreshing advanced statistics...");

        refreshIngredientUsage();
        refreshTasteDistribution();
        refreshTechniqueDistribution();
        refreshTimeCostDistribution();
        refreshActiveUsers();
        refreshQualityAnalysis();
        refreshInteractionTrend();

        log.info("Advanced statistics refresh completed");
    }

    private void refreshIngredientUsage() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getTopIngredients(20);
        LocalDate today = LocalDate.now();
        for (Map<String, Object> item : data) {
            Integer ingredientId = MapValueUtils.getInt(item, "ingredient_id");
            String ingredientName = MapValueUtils.getString(item, "ingredient_name");
            if (ingredientId == null || ingredientName == null || ingredientName.isBlank()) {
                continue;
            }
            IngredientUsageSummary summary = new IngredientUsageSummary();
            summary.setStatDate(today);
            summary.setIngredientId(ingredientId);
            summary.setIngredientName(ingredientName);
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count"));
            statisticsSummaryMapper.saveOrUpdateIngredientUsage(summary);
        }
    }

    private void refreshTasteDistribution() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getTasteDistribution();
        LocalDate today = LocalDate.now();
        for (Map<String, Object> item : data) {
            Integer tasteId = MapValueUtils.getInt(item, "taste_id");
            String tasteName = MapValueUtils.getString(item, "taste_name");
            if (tasteId == null || tasteName == null || tasteName.isBlank()) {
                continue;
            }
            TasteDistributionSummary summary = new TasteDistributionSummary();
            summary.setStatDate(today);
            summary.setTasteId(tasteId);
            summary.setTasteName(tasteName);
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count"));
            statisticsSummaryMapper.saveOrUpdateTasteDistribution(summary);
        }
    }

    private void refreshTechniqueDistribution() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getTechniqueDistribution();
        LocalDate today = LocalDate.now();
        for (Map<String, Object> item : data) {
            Integer techniqueId = MapValueUtils.getInt(item, "technique_id");
            String techniqueName = MapValueUtils.getString(item, "technique_name");
            if (techniqueId == null || techniqueName == null || techniqueName.isBlank()) {
                continue;
            }
            TechniqueDistributionSummary summary = new TechniqueDistributionSummary();
            summary.setStatDate(today);
            summary.setTechniqueId(techniqueId);
            summary.setTechniqueName(techniqueName);
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count"));
            statisticsSummaryMapper.saveOrUpdateTechniqueDistribution(summary);
        }
    }

    private void refreshTimeCostDistribution() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getTimeCostDistribution();
        LocalDate today = LocalDate.now();
        for (Map<String, Object> item : data) {
            Integer timeCostId = MapValueUtils.getInt(item, "time_cost_id");
            String timeCostName = MapValueUtils.getString(item, "time_cost_name");
            if (timeCostId == null || timeCostName == null || timeCostName.isBlank()) {
                continue;
            }
            TimeCostDistributionSummary summary = new TimeCostDistributionSummary();
            summary.setStatDate(today);
            summary.setTimecostId(timeCostId);
            summary.setTimecostName(timeCostName);
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count"));
            statisticsSummaryMapper.saveOrUpdateTimeCostDistribution(summary);
        }
    }

    private void refreshActiveUsers() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getTopActiveUsers(20);
        LocalDate today = LocalDate.now();
        int rank = 1;
        for (Map<String, Object> item : data) {
            Integer userId = MapValueUtils.getInt(item, "user_id");
            String username = MapValueUtils.getString(item, "username");
            if (userId == null || username == null || username.isBlank()) {
                continue;
            }
            ActiveUsersSummary summary = new ActiveUsersSummary();
            summary.setStatDate(today);
            summary.setUserId(userId);
            summary.setUsername(username);
            summary.setRecipeCount(MapValueUtils.getIntOrDefault(item, 0, "recipe_count"));
            summary.setCommentCount(MapValueUtils.getIntOrDefault(item, 0, "comment_count"));
            summary.setTotalScore(MapValueUtils.getIntOrDefault(item, 0, "total_score"));
            summary.setUserRank(rank++);
            statisticsSummaryMapper.saveOrUpdateActiveUser(summary);
        }
    }

    private void refreshQualityAnalysis() {
        Map<String, Object> data = advancedStatisticsMapper.getQualityAnalysis();
        QualityAnalysisSummary summary = new QualityAnalysisSummary();
        summary.setStatDate(LocalDate.now());

        Object highQualityObj = data.get("high_quality_recipes");
        if (highQualityObj instanceof Number) {
            summary.setHighQualityRecipes(((Number) highQualityObj).intValue());
        }

        Object avgLikeObj = data.get("average_like_count");
        if (avgLikeObj instanceof Number) {
            summary.setAverageLikeCount(new java.math.BigDecimal(avgLikeObj.toString()));
        }

        Object avgCommentObj = data.get("average_comment_count");
        if (avgCommentObj instanceof Number) {
            summary.setAverageCommentCount(new java.math.BigDecimal(avgCommentObj.toString()));
        }

        Object zeroInteractionObj = data.get("zero_interaction_recipes");
        if (zeroInteractionObj instanceof Number) {
            summary.setZeroInteractionRecipes(((Number) zeroInteractionObj).intValue());
        }

        summary.setTotalRecipes(statisticsMapper.countTotalRecipes());

        statisticsSummaryMapper.saveOrUpdateQualityAnalysis(summary);
    }

    private void refreshInteractionTrend() {
        List<Map<String, Object>> data = advancedStatisticsMapper.getInteractionTrend();
        for (Map<String, Object> item : data) {
            LocalDate statDate = MapValueUtils.getLocalDate(item, "date");
            if (statDate == null) {
                continue;
            }
            InteractionTrendSummary summary = new InteractionTrendSummary();
            summary.setStatDate(statDate);
            summary.setLikeCount(MapValueUtils.getIntOrDefault(item, 0, "like_count"));
            summary.setFavoriteCount(MapValueUtils.getIntOrDefault(item, 0, "favorite_count"));
            summary.setViewCount(MapValueUtils.getIntOrDefault(item, 0, "view_count"));
            statisticsSummaryMapper.saveOrUpdateInteractionTrend(summary);
        }
    }
}
