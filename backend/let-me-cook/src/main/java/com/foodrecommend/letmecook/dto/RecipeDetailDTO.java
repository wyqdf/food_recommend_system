package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeDetailDTO {
    private Integer id;
    private String title;
    private String image;
    private String difficulty;
    private String time;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer replyCount;
    private List<String> categories;
    private List<IngredientDTO> ingredients;
    private String description;
    private List<StepDTO> steps;
    private NutritionDTO nutrition;
    private String tasteName;
    private String techniqueName;
    private String timeCostName;
    private String difficultyName;
    private String tips;
    private String author;
    private String authorUid;

    @Data
    public static class IngredientDTO {
        private Integer id;
        private String name;
        private String type;
        private String quantity;
    }

    @Data
    public static class StepDTO {
        private Integer step;
        private String description;
        private String image;
    }

    @Data
    public static class NutritionDTO {
        private Integer calories;
        private Integer protein;
        private Integer fat;
        private Integer carbs;
    }
}
