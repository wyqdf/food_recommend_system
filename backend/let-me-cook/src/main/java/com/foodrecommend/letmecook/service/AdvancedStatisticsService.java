package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.admin.AdvancedStatisticsDTO;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.AdvancedStatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsSummaryMapper;
import com.foodrecommend.letmecook.util.MapValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedStatisticsService {

    private final ReentrantLock refreshLock = new ReentrantLock();

    private final AdvancedStatisticsMapper advancedStatisticsMapper;
    private final StatisticsSummaryMapper statisticsSummaryMapper;
    private final JdbcTemplate jdbcTemplate;

    public AdvancedStatisticsDTO getAdvancedStatistics() {
        AdvancedStatisticsDTO dto = new AdvancedStatisticsDTO();

        dto.setTopIngredients(getIngredientStats());
        dto.setTasteDistribution(getTasteStats());
        dto.setTechniqueDistribution(getTechniqueStats());
        dto.setInteractionTrend(getInteractionTrend());
        dto.setTopActiveUsers(getActiveUsers());
        dto.setQualityAnalysis(getQualityAnalysis());

        return dto;
    }

    public List<AdvancedStatisticsDTO.TechniqueStatItem> getDifficultyDistribution() {
        List<DifficultyDistributionSummary> data = statisticsSummaryMapper.getLatestDifficultyDistribution();
        List<AdvancedStatisticsDTO.TechniqueStatItem> result = new ArrayList<>();
        for (DifficultyDistributionSummary item : data) {
            AdvancedStatisticsDTO.TechniqueStatItem stat = new AdvancedStatisticsDTO.TechniqueStatItem();
            stat.setTechniqueId(item.getDifficultyId());
            stat.setTechniqueName(item.getDifficultyName());
            stat.setRecipeCount(item.getRecipeCount());
            result.add(stat);
        }
        return result;
    }

    public List<AdvancedStatisticsDTO.TechniqueStatItem> getTimeCostDistribution() {
        List<TimeCostDistributionSummary> data = statisticsSummaryMapper.getLatestTimeCostDistribution();
        List<AdvancedStatisticsDTO.TechniqueStatItem> result = new ArrayList<>();
        for (TimeCostDistributionSummary item : data) {
            AdvancedStatisticsDTO.TechniqueStatItem stat = new AdvancedStatisticsDTO.TechniqueStatItem();
            stat.setTechniqueId(item.getTimecostId());
            stat.setTechniqueName(item.getTimecostName());
            stat.setRecipeCount(item.getRecipeCount());
            result.add(stat);
        }
        return result;
    }

    public MonthlyTrendDTO getMonthlyTrend() {
        MonthlyTrendDTO dto = new MonthlyTrendDTO();
        LocalDate startDate = LocalDate.now().minusMonths(12);

        List<RecipeTrendDaily> recipeTrend = statisticsSummaryMapper.getRecipeTrendByDateRange(startDate);
        List<UserTrendDaily> userTrend = statisticsSummaryMapper.getUserTrendByDateRange(startDate);
        List<CommentTrendDaily> commentTrend = statisticsSummaryMapper.getCommentTrendByDateRange(startDate);

        dto.setRecipeTrend(convertRecipeTrend(recipeTrend));
        dto.setUserTrend(convertUserTrend(userTrend));
        dto.setCommentTrend(convertCommentTrend(commentTrend));
        return dto;
    }

    private List<AdvancedStatisticsDTO.IngredientStatItem> getIngredientStats() {
        List<IngredientUsageSummary> data = statisticsSummaryMapper.getLatestIngredientUsage(20);
        List<AdvancedStatisticsDTO.IngredientStatItem> result = new ArrayList<>();
        for (IngredientUsageSummary item : data) {
            AdvancedStatisticsDTO.IngredientStatItem stat = new AdvancedStatisticsDTO.IngredientStatItem();
            stat.setIngredientId(item.getIngredientId());
            stat.setIngredientName(item.getIngredientName());
            stat.setRecipeCount(item.getRecipeCount());
            result.add(stat);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.TasteStatItem> getTasteStats() {
        List<TasteDistributionSummary> data = statisticsSummaryMapper.getLatestTasteDistribution();
        List<AdvancedStatisticsDTO.TasteStatItem> result = new ArrayList<>();
        for (TasteDistributionSummary item : data) {
            AdvancedStatisticsDTO.TasteStatItem stat = new AdvancedStatisticsDTO.TasteStatItem();
            stat.setTasteId(item.getTasteId());
            stat.setTasteName(item.getTasteName());
            stat.setRecipeCount(item.getRecipeCount());
            result.add(stat);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.TechniqueStatItem> getTechniqueStats() {
        List<TechniqueDistributionSummary> data = statisticsSummaryMapper.getLatestTechniqueDistribution();
        List<AdvancedStatisticsDTO.TechniqueStatItem> result = new ArrayList<>();
        for (TechniqueDistributionSummary item : data) {
            AdvancedStatisticsDTO.TechniqueStatItem stat = new AdvancedStatisticsDTO.TechniqueStatItem();
            stat.setTechniqueId(item.getTechniqueId());
            stat.setTechniqueName(item.getTechniqueName());
            stat.setRecipeCount(item.getRecipeCount());
            result.add(stat);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.InteractionTrendItem> getInteractionTrend() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        List<InteractionTrendSummary> data = statisticsSummaryMapper.getInteractionTrendByDateRange(startDate);
        List<AdvancedStatisticsDTO.InteractionTrendItem> result = new ArrayList<>();
        for (InteractionTrendSummary item : data) {
            AdvancedStatisticsDTO.InteractionTrendItem trend = new AdvancedStatisticsDTO.InteractionTrendItem();
            trend.setDate(item.getStatDate().toString());
            trend.setLikeCount(item.getLikeCount());
            trend.setFavoriteCount(item.getFavoriteCount());
            trend.setViewCount(item.getViewCount());
            result.add(trend);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.ActiveUserItem> getActiveUsers() {
        List<ActiveUsersSummary> rows = statisticsSummaryMapper.getLatestActiveUsers(20);
        List<AdvancedStatisticsDTO.ActiveUserItem> result = new ArrayList<>();
        for (ActiveUsersSummary row : rows) {
            Integer userId = row.getUserId();
            String username = row.getUsername();
            if (userId == null || username == null || username.isBlank()) {
                continue;
            }
            AdvancedStatisticsDTO.ActiveUserItem user = new AdvancedStatisticsDTO.ActiveUserItem();
            user.setUserId(userId);
            user.setUsername(username);
            user.setRecipeCount(row.getRecipeCount() != null ? row.getRecipeCount() : 0);
            user.setCommentCount(row.getCommentCount() != null ? row.getCommentCount() : 0);
            user.setTotalScore(row.getTotalScore() != null ? row.getTotalScore() : 0);
            result.add(user);
        }
        return result;
    }

    private AdvancedStatisticsDTO.QualityAnalysisItem getQualityAnalysis() {
        QualityAnalysisSummary data = statisticsSummaryMapper.getLatestQualityAnalysis();
        AdvancedStatisticsDTO.QualityAnalysisItem item = new AdvancedStatisticsDTO.QualityAnalysisItem();

        if (data != null) {
            item.setHighQualityRecipes(data.getHighQualityRecipes());
            item.setAverageLikeCount(data.getAverageLikeCount() != null ? data.getAverageLikeCount().doubleValue() : 0);
            item.setAverageCommentCount(
                    data.getAverageCommentCount() != null ? data.getAverageCommentCount().doubleValue() : 0);
            item.setZeroInteractionRecipes(data.getZeroInteractionRecipes());

            if (data.getTotalRecipes() > 0 && data.getHighQualityRecipes() > 0) {
                BigDecimal rate = new BigDecimal(data.getHighQualityRecipes())
                        .multiply(new BigDecimal(100))
                        .divide(new BigDecimal(data.getTotalRecipes()), 2, RoundingMode.HALF_UP);
                item.setQualityRate(rate.doubleValue());
            }
        }

        return item;
    }

    private List<AdvancedStatisticsDTO.InteractionTrendItem> convertRecipeTrend(List<RecipeTrendDaily> data) {
        List<AdvancedStatisticsDTO.InteractionTrendItem> result = new ArrayList<>();
        for (RecipeTrendDaily item : data) {
            AdvancedStatisticsDTO.InteractionTrendItem trend = new AdvancedStatisticsDTO.InteractionTrendItem();
            trend.setDate(item.getStatDate().toString().substring(0, 7));
            trend.setLikeCount(item.getNewRecipesCount());
            result.add(trend);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.InteractionTrendItem> convertUserTrend(List<UserTrendDaily> data) {
        List<AdvancedStatisticsDTO.InteractionTrendItem> result = new ArrayList<>();
        for (UserTrendDaily item : data) {
            AdvancedStatisticsDTO.InteractionTrendItem trend = new AdvancedStatisticsDTO.InteractionTrendItem();
            trend.setDate(item.getStatDate().toString().substring(0, 7));
            trend.setLikeCount(item.getNewUsersCount());
            result.add(trend);
        }
        return result;
    }

    private List<AdvancedStatisticsDTO.InteractionTrendItem> convertCommentTrend(List<CommentTrendDaily> data) {
        List<AdvancedStatisticsDTO.InteractionTrendItem> result = new ArrayList<>();
        for (CommentTrendDaily item : data) {
            AdvancedStatisticsDTO.InteractionTrendItem trend = new AdvancedStatisticsDTO.InteractionTrendItem();
            trend.setDate(item.getStatDate().toString().substring(0, 7));
            trend.setLikeCount(item.getNewCommentsCount());
            result.add(trend);
        }
        return result;
    }

    @lombok.Data
    public static class MonthlyTrendDTO {
        private List<AdvancedStatisticsDTO.InteractionTrendItem> recipeTrend;
        private List<AdvancedStatisticsDTO.InteractionTrendItem> userTrend;
        private List<AdvancedStatisticsDTO.InteractionTrendItem> commentTrend;
    }

    public String refreshAllStatistics() {
        refreshLock.lock();
        try {
            log.info("开始手动刷新所有统计数据...");
            long startTime = System.currentTimeMillis();

            try {
                refreshIngredientUsage();
                refreshTasteDistribution();
                refreshTechniqueDistribution();
                refreshTimeCostDistribution();
                refreshQualityAnalysis();
                refreshInteractionTrend();
                refreshActiveUsers();

                long endTime = System.currentTimeMillis();
                String result = "统计数据刷新完成，耗时 " + (endTime - startTime) + "ms";
                log.info(result);
                return result;
            } catch (Exception e) {
                log.error("手动刷新统计数据失败", e);
                return "刷新失败: " + e.getMessage();
            }
        } finally {
            refreshLock.unlock();
        }
    }

    private void refreshIngredientUsage() {
        log.info("刷新食材使用统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM ingredient_usage_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO ingredient_usage_summary (stat_date, ingredient_id, ingredient_name, recipe_count)
                SELECT ?, ri.ingredient_id, i.name, COUNT(DISTINCT ri.recipe_id) as cnt
                FROM recipe_ingredients ri
                INNER JOIN ingredients i ON ri.ingredient_id = i.id
                GROUP BY ri.ingredient_id, i.name
                ORDER BY cnt DESC
                LIMIT 20
                """;
        jdbcTemplate.update(sql, today);
        log.info("食材使用统计刷新完成");
    }

    private void refreshTasteDistribution() {
        log.info("刷新口味分布统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM taste_distribution_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO taste_distribution_summary (stat_date, taste_id, taste_name, recipe_count)
                SELECT ?, t.id, t.name, COUNT(r.id) as cnt
                FROM tastes t
                LEFT JOIN recipes r ON t.id = r.taste_id
                GROUP BY t.id, t.name
                HAVING cnt > 0
                """;
        jdbcTemplate.update(sql, today);
        log.info("口味分布统计刷新完成");
    }

    private void refreshTechniqueDistribution() {
        log.info("刷新烹饪技法分布统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM technique_distribution_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO technique_distribution_summary (stat_date, technique_id, technique_name, recipe_count)
                SELECT ?, t.id, t.name, COUNT(r.id) as cnt
                FROM techniques t
                LEFT JOIN recipes r ON t.id = r.technique_id
                GROUP BY t.id, t.name
                HAVING cnt > 0
                """;
        jdbcTemplate.update(sql, today);
        log.info("烹饪技法分布统计刷新完成");
    }

    private void refreshTimeCostDistribution() {
        log.info("刷新耗时分布统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM timecost_distribution_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO timecost_distribution_summary (stat_date, timecost_id, timecost_name, recipe_count)
                SELECT ?, tc.id, tc.name, COUNT(r.id) as cnt
                FROM time_costs tc
                LEFT JOIN recipes r ON tc.id = r.time_cost_id
                GROUP BY tc.id, tc.name
                HAVING cnt > 0
                """;
        jdbcTemplate.update(sql, today);
        log.info("耗时分布统计刷新完成");
    }

    private void refreshQualityAnalysis() {
        log.info("刷新质量分析统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM quality_analysis_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO quality_analysis_summary
                (stat_date, high_quality_recipes, average_like_count, average_comment_count, zero_interaction_recipes, total_recipes)
                SELECT
                    ?,
                    (SELECT COUNT(*) FROM recipes WHERE like_count > 100),
                    (SELECT AVG(like_count) FROM recipes),
                    (SELECT AVG(reply_count) FROM recipes),
                    (SELECT COUNT(*) FROM recipes WHERE like_count = 0 AND reply_count = 0),
                    (SELECT COUNT(*) FROM recipes)
                """;
        jdbcTemplate.update(sql, today);
        log.info("质量分析统计刷新完成");
    }

    private void refreshInteractionTrend() {
        log.info("刷新互动趋势统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM interaction_trend_summary WHERE stat_date = ?", today);

        String sql = """
                INSERT INTO interaction_trend_summary (stat_date, like_count, favorite_count, view_count)
                VALUES (?,
                    (SELECT COUNT(*) FROM interactions WHERE interaction_type = 'like' AND create_time >= CURDATE()),
                    (SELECT COUNT(*) FROM interactions WHERE interaction_type = 'favorite' AND create_time >= CURDATE()),
                    (SELECT COUNT(*) FROM behavior_events WHERE event_type = 'recipe_view' AND create_time >= CURDATE()))
                """;
        jdbcTemplate.update(sql, today);
        log.info("互动趋势统计刷新完成");
    }

    private void refreshActiveUsers() {
        log.info("刷新活跃用户统计...");
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("DELETE FROM active_users_summary WHERE stat_date = ?", today);

        List<Map<String, Object>> data = advancedStatisticsMapper.getTopActiveUsers(20);
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
        log.info("活跃用户统计刷新完成，共写入 {} 条", rank - 1);
    }
}
