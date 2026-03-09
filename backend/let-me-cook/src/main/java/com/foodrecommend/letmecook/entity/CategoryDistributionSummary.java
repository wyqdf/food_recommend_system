package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CategoryDistributionSummary {
    private LocalDate statDate;
    private Integer categoryId;
    private String categoryName;
    private Integer recipeCount;
    private LocalDateTime lastUpdated;
}
