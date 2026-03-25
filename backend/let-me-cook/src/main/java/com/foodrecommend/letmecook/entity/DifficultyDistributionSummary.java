package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DifficultyDistributionSummary {
    private LocalDate statDate;
    private Integer difficultyId;
    private String difficultyName;
    private Integer recipeCount;
    private LocalDateTime lastUpdated;
}
