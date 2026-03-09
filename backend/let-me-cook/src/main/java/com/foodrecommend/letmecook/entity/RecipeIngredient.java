package com.foodrecommend.letmecook.entity;

import lombok.Data;

@Data
public class RecipeIngredient {
    private Integer id;
    private Integer recipeId;
    private Integer ingredientId;
    private String ingredientType;
    private String quantity;
    
    private String ingredientName;
}
