package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class AdvancedStatisticsDTO {

    // 食材使用统计
    private List<IngredientStatItem> topIngredients;

    // 口味分布统计
    private List<TasteStatItem> tasteDistribution;

    // 烹饪技法统计
    private List<TechniqueStatItem> techniqueDistribution;

    // 互动趋势分析
    private List<InteractionTrendItem> interactionTrend;

    // 活跃用户统计
    private List<ActiveUserItem> topActiveUsers;

    // 食谱质量分析
    private QualityAnalysisItem qualityAnalysis;

    @Data
    public static class IngredientStatItem {
        private Integer ingredientId;
        private String ingredientName;
        private Integer recipeCount;
        private Double percentage;
    }

    @Data
    public static class TasteStatItem {
        private Integer tasteId;
        private String tasteName;
        private Integer recipeCount;
        private Double percentage;
    }

    @Data
    public static class TechniqueStatItem {
        private Integer techniqueId;
        private String techniqueName;
        private Integer recipeCount;
        private Double percentage;
    }

    @Data
    public static class InteractionTrendItem {
        private String date;
        private Integer likeCount;
        private Integer favoriteCount;
        private Integer viewCount;
    }

    @Data
    public static class ActiveUserItem {
        private Integer userId;
        private String username;
        private Integer recipeCount;
        private Integer commentCount;
        private Integer totalScore;
    }

    @Data
    public static class QualityAnalysisItem {
        private Integer highQualityRecipes; // 高质量食谱数（like_count > 100）
        private Double averageLikeCount; // 平均点赞数
        private Double averageCommentCount; // 平均评论数
        private Integer zeroInteractionRecipes; // 零互动食谱数
        private Double qualityRate; // 高质量食谱占比
    }
}
