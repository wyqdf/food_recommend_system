package com.foodrecommend.letmecook.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyRecipeRecommendation {
    private Long id;
    private Integer userId;
    private LocalDate bizDate;
    private Integer recipeId;
    private Integer rankNo;
    private Integer selectedForDelivery;
    private Double modelScore;
    private String reasonJson;
    private String modelVersion;
    private LocalDateTime createdAt;
}
