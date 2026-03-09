package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class StatsDTO {
    private List<TrendItemDTO> trend;
    private Integer totalNewUsers;
    private List<CategoryDistributionDTO> categoryDistribution;
    private List<DifficultyDistributionDTO> difficultyDistribution;
    private List<TopRecipeDTO> topRecipes;
    private List<TrendItemDTO> commentsTrend;
    private List<TopCommentedRecipeDTO> topCommentedRecipes;
    
    @Data
    public static class TrendItemDTO {
        private String date;
        private Integer count;
    }
    
    @Data
    public static class CategoryDistributionDTO {
        private String name;
        private Integer value;
    }
    
    @Data
    public static class DifficultyDistributionDTO {
        private String name;
        private Integer value;
    }
    
    @Data
    public static class TopRecipeDTO {
        private Integer id;
        private String title;
        private Integer viewCount;
        private Integer likeCount;
    }
    
    @Data
    public static class TopCommentedRecipeDTO {
        private Integer id;
        private String title;
        private Integer commentCount;
    }
}
