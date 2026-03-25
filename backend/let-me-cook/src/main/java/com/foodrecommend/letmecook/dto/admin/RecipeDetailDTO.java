package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class RecipeDetailDTO {
    private Integer id;
    private String title;
    private String author;
    private String authorUid;
    private String image;
    private String description;
    private String tips;
    private String cookware;
    
    private AttributeInfo taste;
    private AttributeInfo technique;
    private AttributeInfo timeCost;
    private AttributeInfo difficulty;
    
    private List<CategoryInfo> categories;
    private List<IngredientInfo> ingredients;
    private List<StepInfo> steps;
    
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
    private String createTime;
    private String updateTime;

    @Data
    public static class AttributeInfo {
        private Integer id;
        private String name;
    }

    @Data
    public static class CategoryInfo {
        private Integer id;
        private String name;
    }

    @Data
    public static class IngredientInfo {
        private Integer id;
        private Integer ingredientId;
        private String name;
        private String type;
        private String quantity;
    }

    @Data
    public static class StepInfo {
        private Integer id;
        private Integer stepNumber;
        private String description;
        private String image;
    }
}
