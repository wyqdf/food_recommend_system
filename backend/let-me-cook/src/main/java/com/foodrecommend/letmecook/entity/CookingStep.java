package com.foodrecommend.letmecook.entity;

import lombok.Data;

@Data
public class CookingStep {
    private Integer id;
    private Integer recipeId;
    private Integer stepNumber;
    private String description;
    private String image;
}
