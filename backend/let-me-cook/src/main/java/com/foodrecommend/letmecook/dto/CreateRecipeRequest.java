package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateRecipeRequest {
    private String title;
    private String author;
    private String authorUid;
    private String image;
    private String description;
    private String tips;
    private String cookware;

    private Integer tasteId;
    private String tasteName;
    private Integer techniqueId;
    private String techniqueName;
    private Integer timeCostId;
    private String timeCostName;
    private Integer difficultyId;
    private String difficultyName;

    private List<Integer> categoryIds;
    private List<String> categoryNames;
    private List<IngredientItem> ingredients;
    private List<StepItem> steps;

    @Data
    public static class IngredientItem {
        private Integer ingredientId;
        private String ingredientName;
        private String type;
        private String quantity;
    }

    @Data
    public static class StepItem {
        private Integer stepNumber;
        private String description;
        private String image;
    }
}
