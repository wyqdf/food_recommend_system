package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IngredientUsageSummary {
    private LocalDate statDate;
    private Integer ingredientId;
    private String ingredientName;
    private Integer recipeCount = 0;
    private LocalDateTime lastUpdated;
}
