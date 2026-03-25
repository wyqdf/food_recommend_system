package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.entity.*;
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
}
