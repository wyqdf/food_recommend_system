package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class StatisticsDTO {
    private List<TrendItem> trend;
    private List<TrendItem> newUsersTrend;
    private List<TrendItem> activeUsersTrend;
    private List<TrendItem> recipesTrend;
    private Integer totalNewUsers;
    private List<DistributionItem> categoryDistribution;
    private List<DistributionItem> difficultyDistribution;
    private List<TopRecipeItem> topRecipes;
    private List<TrendItem> commentsTrend;
    private List<TopCommentedItem> topCommentedRecipes;

    @Data
    public static class TrendItem {
        private String date;
        private Integer count;
    }

    @Data
    public static class DistributionItem {
        private String name;
        private Integer value;
    }

    @Data
    public static class TopRecipeItem {
        private Integer id;
        private String title;
        private Integer viewCount;
        private Integer likeCount;
    }

    @Data
    public static class TopCommentedItem {
        private Integer id;
        private String title;
        private Integer commentCount;
    }
}
