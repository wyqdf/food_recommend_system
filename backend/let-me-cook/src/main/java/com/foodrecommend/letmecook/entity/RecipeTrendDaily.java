package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecipeTrendDaily {
    private LocalDate statDate;
    private Integer newRecipesCount;
    private Integer totalRecipes;
    private LocalDateTime lastUpdated;
}
